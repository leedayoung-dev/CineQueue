
package com.cinequeue.backend.seat.service;

import com.cinequeue.backend.seat.dto.SeatResponse;
import com.cinequeue.backend.seat.repository.SeatRepository;
import com.cinequeue.backend.showtime.repository.ShowtimeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SeatService {

    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;

    public SeatService(
            SeatRepository seatRepository,
            ShowtimeRepository showtimeRepository
    ) {
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
    }

    // Get all seats for a showtime
    public List<SeatResponse> getSeatsByShowtime(Long showtimeId) {

        // Check whether the showtime exists
        if (!showtimeRepository.existsById(showtimeId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Showtime not found"
            );
        }

        return seatRepository
                .findAllByShowtime_IdOrderBySeatRowAscSeatNumberAsc(
                        showtimeId
                )
                .stream()
                .map(SeatResponse::from)
                .toList();
    }
}