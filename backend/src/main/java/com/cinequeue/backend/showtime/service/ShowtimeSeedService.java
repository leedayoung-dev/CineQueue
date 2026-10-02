package com.cinequeue.backend.showtime.service;

import com.cinequeue.backend.movie.entity.Movie;
import com.cinequeue.backend.movie.repository.MovieRepository;
import com.cinequeue.backend.seat.entity.Seat;
import com.cinequeue.backend.seat.repository.SeatRepository;
import com.cinequeue.backend.showtime.TicketPrice;
import com.cinequeue.backend.showtime.dto.ShowtimeGenerateResponse;
import com.cinequeue.backend.showtime.entity.Showtime;
import com.cinequeue.backend.showtime.repository.ShowtimeRepository;
import com.cinequeue.backend.theater.entity.Theater;
import com.cinequeue.backend.theater.service.TheaterDataInitializer;
import com.cinequeue.backend.user.entity.AgeGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowtimeSeedService {

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final TheaterDataInitializer theaterDataInitializer;

    @Transactional
    public ShowtimeGenerateResponse generateShowtimesAndSeats() {
        List<Movie> movies = movieRepository.findAll();

        int skippedMovieCount = 0;
        int createdShowtimeCount = 0;
        int createdSeatCount = 0;

        LocalTime[] showTimes = {
                LocalTime.of(10, 0),
                LocalTime.of(14, 0),
                LocalTime.of(16, 30),
                LocalTime.of(19, 0)
        };

        int movieIndex = 0;
        List<Theater> theaters = theaterDataInitializer.ensureTheaters();

        for (Movie movie : movies) {
            boolean alreadyHasShowtimes =
                    !showtimeRepository
                            .findAllByMovie_IdOrderByStartTimeAsc(movie.getId())
                            .isEmpty();

            if (alreadyHasShowtimes) {
                skippedMovieCount++;
                movieIndex++;
                continue;
            }

            int runningTime = movie.getRunningTime() != null
                    ? movie.getRunningTime()
                    : 120;

            LocalDate showDate = LocalDate.now().plusDays((movieIndex % 7) + 1);

            for (int i = 0; i < showTimes.length; i++) {
                LocalDateTime startTime = LocalDateTime.of(showDate, showTimes[i]);
                LocalDateTime endTime = startTime.plusMinutes(runningTime + 10);

                int theaterNumber = ((movieIndex + i) % 5) + 1;
                int price = TicketPrice.forAge(startTime, AgeGroup.ADULT);

                Showtime showtime = Showtime.builder()
                        .movie(movie)
                        .theaterNumber(theaterNumber)
                        .startTime(startTime)
                        .endTime(endTime)
                        .price(price)
                        .build();

                Showtime savedShowtime = showtimeRepository.save(showtime);
                if (!theaters.isEmpty()) {
                    savedShowtime.assignTheater(theaters.get((movieIndex + i) % theaters.size()));
                }
                createdShowtimeCount++;

                List<Seat> seats = createSeats(savedShowtime);
                seatRepository.saveAll(seats);
                createdSeatCount += seats.size();
            }

            movieIndex++;
        }

        return new ShowtimeGenerateResponse(
                movies.size(),
                skippedMovieCount,
                createdShowtimeCount,
                createdSeatCount
        );
    }

    private List<Seat> createSeats(Showtime showtime) {
        List<Seat> seats = new ArrayList<>();

        String[] rows = {"A", "B", "C", "D", "E", "F"};

        for (String row : rows) {
            for (int number = 1; number <= 8; number++) {
                String seatType = row.equals("E") || row.equals("F")
                        ? "PREMIUM"
                        : "STANDARD";

                Seat seat = Seat.builder()
                        .showtime(showtime)
                        .seatRow(row)
                        .seatNumber(number)
                        .seatType(seatType)
                        .build();

                seats.add(seat);
            }
        }

        return seats;
    }
}
