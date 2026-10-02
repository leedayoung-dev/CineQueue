
package com.cinequeue.backend.booking.service;

import com.cinequeue.backend.booking.dto.BookingRequest;
import com.cinequeue.backend.booking.dto.BookingResponse;
import com.cinequeue.backend.booking.entity.Booking;
import com.cinequeue.backend.booking.entity.BookingStatus;
import com.cinequeue.backend.booking.repository.BookingRepository;
import com.cinequeue.backend.coupon.service.CouponService;
import com.cinequeue.backend.showtime.TicketPrice;
import com.cinequeue.backend.seat.entity.Seat;
import com.cinequeue.backend.seat.entity.SeatStatus;
import com.cinequeue.backend.seat.repository.SeatRepository;
import com.cinequeue.backend.showtime.entity.Showtime;
import com.cinequeue.backend.showtime.repository.ShowtimeRepository;
import com.cinequeue.backend.user.entity.User;
import com.cinequeue.backend.user.repository.UserRepository;
import com.cinequeue.backend.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CouponService couponService;
    private final int seatHoldSeconds;

    public BookingService(
            BookingRepository bookingRepository,
            SeatRepository seatRepository,
            ShowtimeRepository showtimeRepository,
            UserRepository userRepository,
            UserService userService,
            CouponService couponService,
            @Value("${reservation.seat-hold-seconds:180}") int seatHoldSeconds
    ) {
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.couponService = couponService;
        this.seatHoldSeconds = seatHoldSeconds;
    }

    /**
     * 예매 생성
     *
     * 좌석 잠금 -> 좌석 상태 검증 -> 예매 저장
     * 모든 작업은 하나의 트랜잭션에서 처리한다.
     */
    @Transactional
    public BookingResponse createBooking(
            String email,
            BookingRequest request
    ) {
        User user = findUserByEmail(email);

        Showtime showtime = showtimeRepository
                .findById(request.showtimeId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "상영 정보를 찾을 수 없습니다."
                        )
                );

        if (!showtime.getStartTime().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException(
                    "상영이 시작되었거나 종료된 회차입니다."
            );
        }

        List<Long> seatIds = requestedSeatIds(request);
        List<Seat> seats = new ArrayList<>();
        for (Long seatId : seatIds.stream().sorted().toList()) {
            Seat seat = seatRepository
                    .findByIdForUpdate(seatId)
                    .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다."));
            if (!seat.getShowtime().getId().equals(showtime.getId())) {
                throw new IllegalArgumentException("선택한 좌석이 해당 상영 회차에 속하지 않습니다.");
            }
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new IllegalStateException(
                        seat.getStatus() == SeatStatus.HOLD
                                ? "이미 선점된 좌석입니다."
                                : "이미 예매된 좌석입니다."
                );
            }
            seats.add(seat);
        }

        releaseActiveHolds(user.getId());

        for (Seat seat : seats) {
            seat.hold();
        }

        int unitPrice = TicketPrice.forAge(showtime.getStartTime(), user.getAgeGroup());
        Booking booking = new Booking(
                user,
                showtime,
                seats.get(0),
                unitPrice * seats.size(),
                LocalDateTime.now().plusSeconds(seatHoldSeconds)
        );
        for (Seat seat : seats) {
            booking.addSeat(seat);
        }

        // 좌석 변경과 예매 저장이 함께 커밋된다.
        Booking savedBooking = bookingRepository.save(booking);

        return BookingResponse.from(savedBooking);
    }

    /**
     * 로그인한 사용자의 예매 목록 조회
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String email) {
        User user = findUserByEmail(email);

        return bookingRepository
                .findAllByUser_IdAndStatusInOrderByBookedAtDesc(
                        user.getId(),
                        List.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED)
                )
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    /**
     * 예매 상세 조회
     */
    @Transactional(readOnly = true)
    public BookingResponse getBooking(
            Long bookingId,
            String email
    ) {
        User user = findUserByEmail(email);

        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "예매 정보를 찾을 수 없습니다."
                        )
                );

        verifyBookingOwner(booking, user);

        return BookingResponse.from(booking);
    }

    /**
     * 예매 취소
     *
     * 예매 행과 좌석 행을 잠근 뒤 상태를 함께 변경한다.
     */
    @Transactional
    public BookingResponse cancelBooking(
            Long bookingId,
            String email
    ) {
        User user = findUserByEmail(email);

        Booking booking = bookingRepository
                .findByIdForUpdate(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "예매 정보를 찾을 수 없습니다."
                        )
                );

        verifyBookingOwner(booking, user);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException(
                    "이미 취소된 예매입니다."
            );
        }

        if (!booking.getShowtime()
                .getStartTime()
                .isAfter(LocalDateTime.now())) {
            throw new IllegalStateException(
                    "상영 시작 이후에는 예매를 취소할 수 없습니다."
            );
        }

        booking.cancel();
        releaseBookedSeats(booking, false);
        userService.refreshMembershipGrade(user);

        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse payBooking(Long bookingId, String email, Long couponId) {
        User user = findUserByEmail(email);
        Booking booking = lockBooking(bookingId);
        verifyBookingOwner(booking, user);

        if (booking.getStatus() != BookingStatus.HOLD) {
            throw new IllegalStateException("결제할 수 있는 선점이 아닙니다.");
        }
        if (booking.getExpiresAt() == null || !booking.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("선점 시간이 지났습니다.");
        }

        if (couponId != null) {
            couponService.apply(user, couponId, booking);
        }

        booking.confirm();
        reserveBookedSeats(booking);
        userService.refreshMembershipGrade(user);
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse releaseHold(Long bookingId, String email) {
        User user = findUserByEmail(email);
        Booking booking = lockBooking(bookingId);
        verifyBookingOwner(booking, user);

        if (booking.getStatus() != BookingStatus.HOLD) {
            throw new IllegalStateException("해제할 수 있는 선점이 아닙니다.");
        }

        booking.expire();
        releaseBookedSeats(booking, true);
        return BookingResponse.from(booking);
    }

    @Transactional
    public void expireHolds() {
        List<Long> expiredIds = bookingRepository.findExpiredHoldIds(
                BookingStatus.HOLD,
                LocalDateTime.now()
        );

        for (Long bookingId : expiredIds) {
            bookingRepository.findByIdForUpdate(bookingId).ifPresent(booking -> {
                if (booking.getStatus() != BookingStatus.HOLD) {
                    return;
                }
                if (booking.getExpiresAt() == null || booking.getExpiresAt().isAfter(LocalDateTime.now())) {
                    return;
                }
                booking.expire();
                releaseBookedSeats(booking, true);
            });
        }
    }

    private void releaseActiveHolds(Long userId) {
        List<Booking> activeHolds = bookingRepository.findAllByUser_IdAndStatus(userId, BookingStatus.HOLD);
        for (Booking activeHold : activeHolds) {
            Booking lockedHold = bookingRepository.findByIdForUpdate(activeHold.getId()).orElse(null);
            if (lockedHold == null || lockedHold.getStatus() != BookingStatus.HOLD) {
                continue;
            }
            lockedHold.expire();
            releaseBookedSeats(lockedHold, true);
        }
    }

    private List<Long> requestedSeatIds(BookingRequest request) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (request.seatIds() != null) {
            request.seatIds().stream().filter(id -> id != null).forEach(ids::add);
        }
        if (ids.isEmpty() && request.seatId() != null) {
            ids.add(request.seatId());
        }
        if (ids.isEmpty() || ids.size() > 10) {
            throw new IllegalArgumentException("좌석은 1개 이상 10개까지 선택할 수 있습니다.");
        }
        return new ArrayList<>(ids);
    }

    private void reserveBookedSeats(Booking booking) {
        for (Seat seat : booking.reservedSeats()) {
            Seat locked = seatRepository.findByIdForUpdate(seat.getId())
                    .orElseThrow(() -> new IllegalArgumentException("좌석 정보를 찾을 수 없습니다."));
            locked.reserve();
        }
    }

    private void releaseBookedSeats(Booking booking, boolean holdOnly) {
        for (Seat seat : booking.reservedSeats()) {
            Seat locked = seatRepository.findByIdForUpdate(seat.getId()).orElse(null);
            if (locked == null) {
                continue;
            }
            if (!holdOnly || locked.getStatus() == SeatStatus.HOLD) {
                locked.release();
            }
        }
    }

    private Booking lockBooking(Long bookingId) {
        return bookingRepository
                .findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("예매 정보를 찾을 수 없습니다."));
    }

    /**
     * 이메일로 사용자 조회
     */
    private User findUserByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );
    }

    /**
     * 본인 예매인지 확인
     */
    private void verifyBookingOwner(
            Booking booking,
            User user
    ) {
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "본인의 예매만 조회하거나 취소할 수 있습니다."
            );
        }
    }
}