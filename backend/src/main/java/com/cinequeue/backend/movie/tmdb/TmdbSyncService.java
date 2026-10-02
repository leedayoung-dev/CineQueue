package com.cinequeue.backend.movie.tmdb;

import com.cinequeue.backend.movie.entity.Movie;
import com.cinequeue.backend.movie.repository.MovieRepository;
import com.cinequeue.backend.movie.tmdb.dto.TmdbMovieDetailResponse;
import com.cinequeue.backend.movie.tmdb.dto.TmdbMovieListResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TmdbSyncService {

    private final TmdbApiService tmdbApiService;
    private final MovieRepository movieRepository;

    public TmdbSyncService(
            TmdbApiService tmdbApiService,
            MovieRepository movieRepository
    ) {
        this.tmdbApiService = tmdbApiService;
        this.movieRepository = movieRepository;
    }

    @Transactional
    public Map<String, Object> syncPopularMovies(int page) {
        if (page < 1 || page > 3) {
            throw new IllegalArgumentException(
                    "page는 1~3 사이의 값이어야 합니다."
            );
        }

        TmdbMovieListResponse response =
                tmdbApiService.getPopularMovies(page);

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        if (response == null || response.results() == null) {
            throw new IllegalStateException(
                    "TMDB 영화 목록을 가져오지 못했습니다."
            );
        }

        for (TmdbMovieListResponse.TmdbMovie item : response.results()) {
            if (item.id() == null) {
                skipped++;
                continue;
            }

            TmdbMovieDetailResponse detail =
                    tmdbApiService.getMovieDetail(item.id());

            if (detail == null || detail.runtime() == null
                    || detail.runtime() <= 0) {
                skipped++;
                continue;
            }

            LocalDate releaseDate = parseDate(detail.releaseDate());

            Movie existing = movieRepository
                    .findByTmdbId(detail.id())
                    .orElse(null);

            if (existing != null) {
                existing.updateFromTmdb(
                        detail.title(),
                        detail.overview(),
                        releaseDate,
                        detail.posterPath(),
                        detail.voteAverage()
                );
                updated++;
            } else {
                Movie movie = Movie.builder()
                        .tmdbId(detail.id())
                        .title(detail.title())
                        .description(detail.overview())
                        .runningTime(detail.runtime())
                        .ageRating("미정")
                        .releaseDate(releaseDate)
                        .posterPath(detail.posterPath())
                        .voteAverage(detail.voteAverage())
                        .build();

                movieRepository.save(movie);
                inserted++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", page);
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("skipped", skipped);

        return result;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}