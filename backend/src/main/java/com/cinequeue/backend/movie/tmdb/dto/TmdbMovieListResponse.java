package com.cinequeue.backend.movie.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TmdbMovieListResponse(
        List<TmdbMovie> results
) {
    public record TmdbMovie(
            Long id,
            String title,

            @JsonProperty("overview")
            String description,

            @JsonProperty("release_date")
            String releaseDate,

            @JsonProperty("poster_path")
            String posterPath,

            @JsonProperty("vote_average")
            Double voteAverage
    ) {
    }
}