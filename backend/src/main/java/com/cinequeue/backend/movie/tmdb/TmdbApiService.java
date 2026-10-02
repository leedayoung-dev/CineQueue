package com.cinequeue.backend.movie.tmdb;

import com.cinequeue.backend.movie.tmdb.dto.TmdbMovieDetailResponse;
import com.cinequeue.backend.movie.tmdb.dto.TmdbMovieListResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TmdbApiService {

    private final RestClient restClient;

    public TmdbApiService(
            RestClient.Builder builder,
            @Value("${tmdb.api.base-url}") String baseUrl,
            @Value("${tmdb.api.read-access-token}") String token
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }

    // 인기 영화 목록 조회
    public TmdbMovieListResponse getPopularMovies(int page) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/popular")
                        .queryParam("language", "ko-KR")
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }

    // 영화별 상세 정보 조회
    public TmdbMovieDetailResponse getMovieDetail(Long tmdbId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam("language", "ko-KR")
                        .build(tmdbId))
                .retrieve()
                .body(TmdbMovieDetailResponse.class);
    }
}