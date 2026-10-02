package com.cinequeue.backend.movie.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "movies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TMDB 영화 ID. 기존 영화는 null일 수 있음.
    @Column(unique = true)
    private Long tmdbId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer runningTime;

    @Column(nullable = false, length = 20)
    private String ageRating;

    private LocalDate releaseDate;

    // TMDB 포스터 경로
    @Column(length = 500)
    private String posterPath;

    // TMDB 평점
    private Double voteAverage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Movie(
            Long tmdbId,
            String title,
            String description,
            Integer runningTime,
            String ageRating,
            LocalDate releaseDate,
            String posterPath,
            Double voteAverage
    ) {
        this.tmdbId = tmdbId;
        this.title = title;
        this.description = description;
        this.runningTime = runningTime;
        this.ageRating = ageRating;
        this.releaseDate = releaseDate;
        this.posterPath = posterPath;
        this.voteAverage = voteAverage;
    }

    // TMDB에서 가져온 정보로 기존 영화 데이터 갱신
    public void updateFromTmdb(
            String title,
            String description,
            LocalDate releaseDate,
            String posterPath,
            Double voteAverage
    ) {
        this.title = title;
        this.description = description;
        this.releaseDate = releaseDate;
        this.posterPath = posterPath;
        this.voteAverage = voteAverage;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}