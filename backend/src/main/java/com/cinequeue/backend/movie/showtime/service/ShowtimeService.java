
package com.cinequeue.backend.showtime.service;

import com.cinequeue.backend.showtime.dto.ShowtimeResponse;
import com.cinequeue.backend.showtime.entity.Showtime;
import com.cinequeue.backend.showtime.repository.ShowtimeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;

    public ShowtimeService(ShowtimeRepository showtimeRepository) {
        this.showtimeRepository = showtimeRepository;
    }

    public List<ShowtimeResponse> getUpcomingShowtimes() {
        return showtimeRepository
                .findAllByStartTimeGreaterThanEqualOrderByStartTimeAsc(LocalDateTime.now())
                .stream()
                .map(ShowtimeResponse::from)
                .toList();
    }

    // 특정 영화의 상영 일정 목록 조회
    public List<ShowtimeResponse> getShowtimesByMovie(Long movieId) {
        return showtimeRepository
                .findAllByMovie_IdAndSourceKeyIsNotNullAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
                        movieId,
                        LocalDateTime.now()
                )
                .stream()
                .map(ShowtimeResponse::from)
                .toList();
    }

    // 상영 일정 상세 조회
    public ShowtimeResponse getShowtime(Long id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "상영 일정을 찾을 수 없습니다."
                ));

        return ShowtimeResponse.from(showtime);
    }
}