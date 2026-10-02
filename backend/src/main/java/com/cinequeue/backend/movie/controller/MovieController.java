package com.cinequeue.backend.movie.controller;

import com.cinequeue.backend.movie.dto.MovieResponse;
import com.cinequeue.backend.movie.service.MovieService;
import com.cinequeue.backend.movie.tmdb.TmdbSyncService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;
    private final TmdbSyncService tmdbSyncService;

    public MovieController(
            MovieService movieService,
            TmdbSyncService tmdbSyncService
    ) {
        this.movieService = movieService;
        this.tmdbSyncService = tmdbSyncService;
    }

    // 전체 영화 목록 조회
    @GetMapping
    public ResponseEntity<List<MovieResponse>> getMovies() {
        return ResponseEntity.ok(movieService.getMovies());
    }

    // 특정 영화 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovie(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(movieService.getMovie(id));
    }

    // TMDB 인기 영화 정보를 가져와 DB에 저장
    @PostMapping("/tmdb/sync")
    public ResponseEntity<Map<String, Object>> syncTmdbMovies(
            @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(
                tmdbSyncService.syncPopularMovies(page)
        );
    }
}