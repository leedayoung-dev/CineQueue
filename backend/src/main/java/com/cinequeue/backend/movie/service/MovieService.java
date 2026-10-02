
package com.cinequeue.backend.movie.service;

import com.cinequeue.backend.movie.dto.MovieResponse;
import com.cinequeue.backend.movie.entity.Movie;
import com.cinequeue.backend.movie.repository.MovieRepository;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    // 전체 영화 목록 조회
    public List<MovieResponse> getMovies() {
        return movieRepository
                .findAll(Sort.by(Sort.Direction.DESC, "releaseDate"))
                .stream()
                .map(MovieResponse::from)
                .toList();
    }

    // 영화 ID로 상세 조회
    public MovieResponse getMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 영화를 찾을 수 없습니다."
                ));

        return MovieResponse.from(movie);
    }
}