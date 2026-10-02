
package com.cinequeue.backend.showtime.entity;

import com.cinequeue.backend.movie.entity.Movie;
import com.cinequeue.backend.theater.entity.Theater;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "showtimes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Showtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상영 일정이 어떤 영화에 속하는지 연결
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    // 상영관 번호
    @Column(nullable = false)
    private Integer theaterNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id")
    private Theater theater;

    // 상영 시작 시간
    @Column(nullable = false)
    private LocalDateTime startTime;

    // 상영 종료 시간
    @Column(nullable = false)
    private LocalDateTime endTime;

    // 티켓 가격 (원)
    @Column(nullable = false)
    private Integer price;

    @Column(name = "source_key", length = 120, unique = true)
    private String sourceKey;

    private Boolean referenceLayout;

    @Builder
    public Showtime(
            Movie movie,
            Integer theaterNumber,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer price
    ) {
        this.movie = movie;
        this.theaterNumber = theaterNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
    }

    public void assignTheater(Theater theater) {
        this.theater = theater;
    }

    public void assignSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    public boolean isReferenceLayout() {
        return Boolean.TRUE.equals(referenceLayout);
    }

    public void markReferenceLayout() {
        this.referenceLayout = true;
    }
}