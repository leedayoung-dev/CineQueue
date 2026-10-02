package com.cinequeue.backend.movie.dto;

import com.cinequeue.backend.movie.entity.Movie;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MovieResponse(
        Long id,
        Long tmdbId,
        String title,
        String description,
        Integer runningTime,
        String ageRating,
        LocalDate releaseDate,
        String posterPath,
        Double voteAverage,
        LocalDateTime createdAt
) {
    public static MovieResponse from(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTmdbId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getRunningTime(),
                movie.getAgeRating(),
                movie.getReleaseDate(),
                movie.getPosterPath(),
                movie.getVoteAverage(),
                movie.getCreatedAt()
        );
    }
}