
package com.cinequeue.backend.showtime.dto;

import com.cinequeue.backend.showtime.TicketPrice;
import com.cinequeue.backend.showtime.entity.Showtime;
import com.cinequeue.backend.user.entity.AgeGroup;

import java.time.LocalDateTime;

public record ShowtimeResponse(
        Long id,
        Long movieId,
        String movieTitle,
        String theaterName,
        Integer theaterNumber,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer price,
        Integer childPrice,
        Integer teenPrice,
        Integer adultPrice,
        boolean referenceLayout
) {

    public static ShowtimeResponse from(Showtime showtime) {
        return new ShowtimeResponse(
                showtime.getId(),
                showtime.getMovie().getId(),
                showtime.getMovie().getTitle(),
                showtime.getTheater() == null ? null : showtime.getTheater().getName(),
                showtime.getTheaterNumber(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getPrice(),
                TicketPrice.forAge(showtime.getStartTime(), AgeGroup.CHILD),
                TicketPrice.forAge(showtime.getStartTime(), AgeGroup.TEEN),
                TicketPrice.forAge(showtime.getStartTime(), AgeGroup.ADULT),
                showtime.isReferenceLayout()
        );
    }
}