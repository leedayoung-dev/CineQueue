
package com.cinequeue.backend.showtime.controller;

import com.cinequeue.backend.showtime.dto.ShowtimeGenerateResponse;
import com.cinequeue.backend.showtime.dto.ShowtimeResponse;
import com.cinequeue.backend.showtime.service.ShowtimeSeedService;
import com.cinequeue.backend.showtime.service.ShowtimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ShowtimeController {

    private final ShowtimeService showtimeService;
    private final ShowtimeSeedService showtimeSeedService;

    public ShowtimeController(
            ShowtimeService showtimeService,
            ShowtimeSeedService showtimeSeedService
    ) {
        this.showtimeService = showtimeService;
        this.showtimeSeedService = showtimeSeedService;
    }

    @GetMapping("/showtimes")
    public ResponseEntity<List<ShowtimeResponse>> getShowtimes() {
        return ResponseEntity.ok(showtimeService.getUpcomingShowtimes());
    }

    // 특정 영화의 상영 일정 목록
    @GetMapping("/movies/{movieId}/showtimes")
    public ResponseEntity<List<ShowtimeResponse>> getShowtimesByMovie(
            @PathVariable Long movieId
    ) {
        return ResponseEntity.ok(
                showtimeService.getShowtimesByMovie(movieId)
        );
    }

    // 상영 일정 상세
    @GetMapping("/showtimes/{id}")
    public ResponseEntity<ShowtimeResponse> getShowtime(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                showtimeService.getShowtime(id)
        );
    }

    // 상영 회차와 좌석 생성
    @PostMapping("/showtimes/generate")
    public ResponseEntity<ShowtimeGenerateResponse> generateShowtimes() {
        return ResponseEntity.ok(
                showtimeSeedService.generateShowtimesAndSeats()
        );
    }
}