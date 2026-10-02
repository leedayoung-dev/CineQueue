
package com.cinequeue.backend.seat.dto;

import com.cinequeue.backend.seat.entity.Seat;

public record SeatResponse(
        Long id,
        Long showtimeId,
        String seatRow,
        Integer seatNumber,
        String seatType,
        String seatLabel,
        String status
) {
    public static SeatResponse from(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getShowtime().getId(),
                seat.getSeatRow(),
                seat.getSeatNumber(),
                seat.getSeatType(),
                seat.getSeatRow() + seat.getSeatNumber(),
                seat.getStatus().name()
        );
    }
}