package com.cinequeue.backend.theater.controller;

import com.cinequeue.backend.showtime.dto.ShowtimeResponse;
import com.cinequeue.backend.theater.dto.TheaterResponse;
import com.cinequeue.backend.theater.schedule.CinemaScheduleSyncService;
import com.cinequeue.backend.theater.service.TheaterService;
import com.cinequeue.backend.user.entity.Region;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/theaters")
public class TheaterController {

    private final TheaterService theaterService;
    private final CinemaScheduleSyncService cinemaScheduleSyncService;

    public TheaterController(
            TheaterService theaterService,
            CinemaScheduleSyncService cinemaScheduleSyncService
    ) {
        this.theaterService = theaterService;
        this.cinemaScheduleSyncService = cinemaScheduleSyncService;
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<TheaterResponse>> nearby(
            @RequestParam(required = false) Region region,
            @RequestParam(required = false) String district
    ) {
        return ResponseEntity.ok(theaterService.nearby(region, district));
    }

    @GetMapping("/{theaterId}/showtimes")
    public ResponseEntity<List<ShowtimeResponse>> showtimes(
            @PathVariable Long theaterId
    ) {
        cinemaScheduleSyncService.syncTheater(theaterId);
        return ResponseEntity.ok(theaterService.upcomingShowtimes(theaterId));
    }
}
