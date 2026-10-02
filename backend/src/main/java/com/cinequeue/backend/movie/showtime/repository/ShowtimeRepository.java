
package com.cinequeue.backend.showtime.repository;

import com.cinequeue.backend.showtime.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShowtimeRepository
        extends JpaRepository<Showtime, Long> {

    // 특정 영화의 상영 일정을 시작 시간 오름차순으로 조회
    List<Showtime> findAllByMovie_IdOrderByStartTimeAsc(Long movieId);

    List<Showtime> findAllByMovie_IdAndSourceKeyIsNotNullAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
            Long movieId,
            LocalDateTime startTime
    );

    List<Showtime> findAllByStartTimeGreaterThanEqualOrderByStartTimeAsc(LocalDateTime startTime);

    List<Showtime> findAllByTheater_IdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
            Long theaterId,
            LocalDateTime startTime
    );

    List<Showtime> findAllByTheaterIsNull();

    Optional<Showtime> findBySourceKey(String sourceKey);

    boolean existsByTheater_IdAndSourceKeyIsNotNull(Long theaterId);

    List<Showtime> findAllByTheater_IdAndSourceKeyIsNullAndStartTimeGreaterThanEqual(
            Long theaterId,
            LocalDateTime startTime
    );

    // 특정 상영 일정 조회
    Optional<Showtime> findById(Long id);
}