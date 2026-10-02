package com.cinequeue.backend.booking;

import com.cinequeue.backend.booking.entity.Booking;
import com.cinequeue.backend.booking.entity.BookingStatus;
import com.cinequeue.backend.coupon.entity.Coupon;
import com.cinequeue.backend.coupon.entity.CouponStatus;
import com.cinequeue.backend.coupon.entity.CouponType;
import com.cinequeue.backend.seat.entity.Seat;
import com.cinequeue.backend.seat.entity.SeatStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationRulesTest {

    @Test
    void seatCanBeHeldOnceAndThenReserved() {
        Seat seat = Seat.builder()
                .seatRow("A")
                .seatNumber(1)
                .seatType("STANDARD")
                .build();

        seat.hold();
        assertEquals(SeatStatus.HOLD, seat.getStatus());
        assertThrows(IllegalStateException.class, seat::hold);

        seat.reserve();
        assertEquals(SeatStatus.RESERVED, seat.getStatus());
        assertThrows(IllegalStateException.class, seat::reserve);
    }

    @Test
    void referenceOccupiedSeatCannotBeHeld() {
        Seat seat = Seat.builder()
                .seatRow("B")
                .seatNumber(2)
                .seatType("STANDARD")
                .build();

        seat.markReferenceOccupied();

        assertThrows(IllegalStateException.class, seat::hold);
    }

    @Test
    void holdCanBeConfirmedOnceAndThenCancelled() {
        Booking booking = new Booking(null, null, null, 12000, LocalDateTime.now().plusMinutes(3));

        booking.confirm();
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertThrows(IllegalStateException.class, booking::confirm);
        assertThrows(IllegalStateException.class, booking::expire);

        booking.cancel();
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void expiredHoldCannotBeConfirmed() {
        Booking booking = new Booking(null, null, null, 12000, LocalDateTime.now().minusSeconds(1));

        booking.expire();

        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertThrows(IllegalStateException.class, booking::confirm);
    }

    @Test
    void couponDiscountCannotExceedPriceAndCannotBeReused() {
        Booking booking = new Booking(null, null, null, 900, LocalDateTime.now().plusMinutes(3));
        Coupon coupon = new Coupon(null, CouponType.WELCOME, LocalDateTime.now());

        booking.applyDiscount(coupon.getDiscountAmount());
        assertEquals(0, booking.getPaidPrice());

        coupon.use(booking, LocalDateTime.now());
        assertEquals(CouponStatus.USED, coupon.getStatus());
        assertThrows(IllegalStateException.class, () -> coupon.use(booking, LocalDateTime.now()));
    }

    @Test
    void expiredCouponCannotBeUsed() {
        Coupon coupon = new Coupon(null, CouponType.WELCOME, LocalDateTime.now().minusDays(31));

        assertThrows(IllegalStateException.class, () -> coupon.use(null, LocalDateTime.now()));
    }
}
