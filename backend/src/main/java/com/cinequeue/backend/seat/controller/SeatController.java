
package com.cinequeue.backend.seat.controller;

import com.cinequeue.backend.seat.dto.SeatResponse;
import com.cinequeue.backend.seat.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/showtimes/{showtimeId}/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    // Get seats for a specific showtime
    @GetMapping
    public ResponseEntity<List<SeatResponse>> getSeatsByShowtime(
            @PathVariable Long showtimeId
    ) {
        return ResponseEntity.ok(
                seatService.getSeatsByShowtime(showtimeId)
        );
    }
}