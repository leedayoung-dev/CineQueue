package com.cinequeue.backend.booking.dto;

import com.cinequeue.backend.booking.entity.Booking;
import com.cinequeue.backend.seat.entity.Seat;

import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
    Long bookingId,
    Long showtimeId,
    String movieTitle,
    String posterPath,
    String seatLabel,
    LocalDateTime startTime,
    Integer price,
    String status,
    LocalDateTime bookedAt,
    Long seatId,
    LocalDateTime expiresAt,
    List<Long> seatIds
) {
    public static BookingResponse from(Booking booking) {
        List<Seat> seats = booking.reservedSeats();
        String labels = seats.stream()
                .map(seat -> seat.getSeatRow() + seat.getSeatNumber())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        List<Long> seatIds = seats.stream().map(Seat::getId).toList();
        return new BookingResponse(
            booking.getId(),
            booking.getShowtime().getId(),
            booking.getShowtime().getMovie().getTitle(),
            booking.getShowtime().getMovie().getPosterPath(),
            labels,
            booking.getShowtime().getStartTime(),
            booking.getPaidPrice() != null
                    ? booking.getPaidPrice()
                    : booking.getShowtime().getPrice(),
            booking.getStatus().name(),
            booking.getBookedAt(),
            seatIds.isEmpty() ? null : seatIds.get(0),
            booking.getExpiresAt(),
            seatIds
        );
    }
}