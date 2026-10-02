package com.cinequeue.backend.booking.entity;

import com.cinequeue.backend.seat.entity.Seat;
import com.cinequeue.backend.showtime.entity.Showtime;
import com.cinequeue.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;

    @Column(name = "paid_price")
    private Integer paidPrice;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> bookedSeats = new ArrayList<>();

    public Booking(User user, Showtime showtime, Seat seat, int paidPrice, LocalDateTime expiresAt) {
        this.user = user;
        this.showtime = showtime;
        this.seat = seat;
        this.status = BookingStatus.HOLD;
        this.bookedAt = LocalDateTime.now();
        this.paidPrice = paidPrice;
        this.expiresAt = expiresAt;
    }

    public void addSeat(Seat nextSeat) {
        this.bookedSeats.add(new BookingSeat(this, nextSeat));
        if (this.seat == null) {
            this.seat = nextSeat;
        }
    }

    public List<Seat> reservedSeats() {
        if (this.bookedSeats != null && !this.bookedSeats.isEmpty()) {
            return this.bookedSeats.stream().map(BookingSeat::getSeat).toList();
        }
        return this.seat == null ? List.of() : List.of(this.seat);
    }

    public void confirm() {
        if (this.status != BookingStatus.HOLD) {
            throw new IllegalStateException("결제할 수 있는 선점이 아닙니다.");
        }
        this.status = BookingStatus.CONFIRMED;
        this.bookedAt = LocalDateTime.now();
        this.expiresAt = null;
    }

    public void applyDiscount(int discountAmount) {
        int price = this.paidPrice == null ? 0 : this.paidPrice;
        this.paidPrice = Math.max(0, price - discountAmount);
    }

    public void expire() {
        if (this.status != BookingStatus.HOLD) {
            throw new IllegalStateException("해제할 수 있는 선점이 아닙니다.");
        }
        this.status = BookingStatus.EXPIRED;
    }

    public void cancel() {
        if (this.status == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        if (this.status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("확정된 예매만 취소할 수 있습니다.");
        }
        this.status = BookingStatus.CANCELLED;
    }
}