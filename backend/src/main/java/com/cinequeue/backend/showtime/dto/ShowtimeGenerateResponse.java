package com.cinequeue.backend.showtime.dto;

public record ShowtimeGenerateResponse(
        int movieCount,
        int skippedMovieCount,
        int createdShowtimeCount,
        int createdSeatCount
) {
}
