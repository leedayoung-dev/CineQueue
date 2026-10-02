package com.cinequeue.backend.theater.schedule;

public record CinemaScreening(
        String sourceKey,
        String movieName,
        String playDate,
        String startTime,
        String endTime,
        String screenName,
        Integer totalSeats,
        Integer remainingSeats
) {
}
