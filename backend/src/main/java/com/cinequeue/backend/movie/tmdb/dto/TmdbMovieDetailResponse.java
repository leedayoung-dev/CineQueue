package com.cinequeue.backend.movie.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbMovieDetailResponse(
        Long id,
        String title,
        String overview,

        @JsonProperty("release_date")
        String releaseDate,

        @JsonProperty("poster_path")
        String posterPath,

        @JsonProperty("vote_average")
        Double voteAverage,

        Integer runtime
) {
}