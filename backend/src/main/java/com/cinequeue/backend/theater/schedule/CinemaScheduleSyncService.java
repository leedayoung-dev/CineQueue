package com.cinequeue.backend.theater.schedule;

import com.cinequeue.backend.booking.repository.BookingRepository;
import com.cinequeue.backend.movie.entity.Movie;
import com.cinequeue.backend.movie.repository.MovieRepository;
import com.cinequeue.backend.seat.entity.Seat;
import com.cinequeue.backend.seat.entity.SeatStatus;
import com.cinequeue.backend.seat.repository.SeatRepository;
import com.cinequeue.backend.showtime.TicketPrice;
import com.cinequeue.backend.showtime.entity.Showtime;
import com.cinequeue.backend.showtime.repository.ShowtimeRepository;
import com.cinequeue.backend.theater.entity.CinemaChain;
import com.cinequeue.backend.theater.entity.Theater;
import com.cinequeue.backend.theater.repository.TheaterRepository;
import com.cinequeue.backend.user.entity.AgeGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CinemaScheduleSyncService {

    private static final Logger log = LoggerFactory.getLogger(CinemaScheduleSyncService.class);
    private static final DateTimeFormatter PLAY_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final long CACHE_MILLIS = 10 * 60 * 1000L;
    private static final int SEATS_PER_ROW = 14;
    private static final int MAX_ROWS = 26;
    private static final int MAX_SEATS = MAX_ROWS * 24;

    private final TheaterRepository theaterRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final CinemaScheduleClient cinemaScheduleClient;
    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate newTransactionTemplate;
    private final ConcurrentHashMap<Long, Long> syncedAt = new ConcurrentHashMap<>();

    public CinemaScheduleSyncService(
            TheaterRepository theaterRepository,
            MovieRepository movieRepository,
            ShowtimeRepository showtimeRepository,
            SeatRepository seatRepository,
            BookingRepository bookingRepository,
            CinemaScheduleClient cinemaScheduleClient,
            PlatformTransactionManager transactionManager
    ) {
        this.theaterRepository = theaterRepository;
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
        this.cinemaScheduleClient = cinemaScheduleClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.newTransactionTemplate = requiresNew;
    }

    public void syncAllChainTheaters() {
        List<Long> theaterIds = theaterRepository.findAll().stream()
                .filter(theater -> CinemaChain.fromName(theater.getName()) != null)
                .map(Theater::getId)
                .toList();
        for (Long theaterId : theaterIds) {
            syncTheater(theaterId);
        }
    }

    public void syncTheater(Long theaterId) {
        Long cached = syncedAt.get(theaterId);
        if (cached != null && System.currentTimeMillis() - cached < CACHE_MILLIS) {
            return;
        }
        Theater theater = theaterRepository.findById(theaterId).orElse(null);
        if (theater == null) {
            return;
        }
        CinemaChain chain = CinemaChain.fromName(theater.getName());
        if (chain == null) {
            return;
        }
        String keyword = CinemaChain.keyword(theater.getName());
        LocalDate today = LocalDate.now();
        List<CinemaScreening> screenings = new ArrayList<>();
        boolean fetched = false;
        for (int day = 0; day < 2; day++) {
            List<CinemaScreening> daily = cinemaScheduleClient.fetch(chain, keyword, today.plusDays(day));
            if (!daily.isEmpty()) {
                fetched = true;
                screenings.addAll(daily);
            }
        }
        if (!fetched) {
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> assignChain(theaterId, chain));
            Map<String, Long> moviesByTitle = new HashMap<>();
            for (Movie movie : movieRepository.findAll()) {
                moviesByTitle.putIfAbsent(normalize(movie.getTitle()), movie.getId());
            }
            int savedCount = 0;
            for (CinemaScreening screening : screenings) {
                Long movieId = match(moviesByTitle, screening.movieName());
                if (movieId == null) {
                    continue;
                }
                try {
                    Boolean created = newTransactionTemplate.execute(status -> insertShowtime(theaterId, movieId, screening));
                    if (Boolean.TRUE.equals(created)) {
                        savedCount++;
                    }
                } catch (DataIntegrityViolationException exception) {
                    log.debug("Cinema showtime already stored: {}", screening.sourceKey());
                }
            }
            if (savedCount > 0 || Boolean.TRUE.equals(transactionTemplate.execute(status ->
                    showtimeRepository.existsByTheater_IdAndSourceKeyIsNotNull(theaterId)))) {
                transactionTemplate.executeWithoutResult(status -> removePlaceholderShowtimes(theaterId));
            }
            syncedAt.put(theaterId, System.currentTimeMillis());
        } catch (RuntimeException exception) {
            log.warn("Failed to save cinema schedule for theater {}: {}", theaterId, exception.getMessage());
        }
    }

    private void assignChain(Long theaterId, CinemaChain chain) {
        Theater theater = theaterRepository.findById(theaterId).orElse(null);
        if (theater != null && theater.getChain() != chain) {
            theater.assignChain(chain);
        }
    }

    private boolean insertShowtime(Long theaterId, Long movieId, CinemaScreening screening) {
        Showtime existing = showtimeRepository.findBySourceKey(screening.sourceKey()).orElse(null);
        if (existing != null) {
            applyReferenceLayout(existing, screening);
            return false;
        }
        Theater theater = theaterRepository.findById(theaterId).orElse(null);
        Movie movie = movieRepository.findById(movieId).orElse(null);
        if (theater == null || movie == null) {
            return false;
        }
        LocalDateTime start = parseDateTime(screening.playDate(), screening.startTime());
        if (start == null) {
            return false;
        }
        LocalDateTime end = parseDateTime(screening.playDate(), screening.endTime());
        if (end == null || !end.isAfter(start)) {
            int runningTime = movie.getRunningTime() == null ? 120 : movie.getRunningTime();
            end = start.plusMinutes(runningTime + 10L);
        }
        Showtime showtime = Showtime.builder()
                .movie(movie)
                .theaterNumber(screenNumber(screening.screenName()))
                .startTime(start)
                .endTime(end)
                .price(TicketPrice.forAge(start, AgeGroup.ADULT))
                .build();
        showtime.assignTheater(theater);
        showtime.assignSourceKey(screening.sourceKey());
        Showtime saved = showtimeRepository.save(showtime);
        List<Seat> seats = createSeats(saved, screening.totalSeats());
        markReferenceSeats(seats, screening.sourceKey(), screening.totalSeats(), screening.remainingSeats());
        if (screening.totalSeats() != null && screening.remainingSeats() != null && screening.totalSeats() > 0) {
            saved.markReferenceLayout();
        }
        seatRepository.saveAll(seats);
        return true;
    }

    private void applyReferenceLayout(Showtime showtime, CinemaScreening screening) {
        if (screening.totalSeats() == null || screening.remainingSeats() == null || screening.totalSeats() <= 0) {
            return;
        }
        if (bookingRepository.existsByShowtime_Id(showtime.getId())) {
            return;
        }
        int target = Math.min(screening.totalSeats(), MAX_SEATS);
        List<Seat> seats = seatRepository.findAllByShowtime_IdOrderBySeatRowAscSeatNumberAsc(showtime.getId());
        if (seats.size() == target && showtime.isReferenceLayout()) {
            return;
        }
        if (seats.size() != target) {
            if (!seats.isEmpty()) {
                seatRepository.deleteAll(seats);
                seatRepository.flush();
            }
            seats = createSeats(showtime, screening.totalSeats());
            markReferenceSeats(seats, screening.sourceKey(), screening.totalSeats(), screening.remainingSeats());
            seatRepository.saveAll(seats);
            showtime.markReferenceLayout();
            return;
        }
        if (seats.isEmpty() || seats.stream().anyMatch(seat -> seat.getStatus() != SeatStatus.AVAILABLE)) {
            return;
        }
        markReferenceSeats(seats, screening.sourceKey(), screening.totalSeats(), screening.remainingSeats());
        showtime.markReferenceLayout();
    }

    private void removePlaceholderShowtimes(Long theaterId) {
        List<Showtime> placeholders = showtimeRepository
                .findAllByTheater_IdAndSourceKeyIsNullAndStartTimeGreaterThanEqual(theaterId, LocalDateTime.now());
        for (Showtime showtime : placeholders) {
            if (bookingRepository.existsByShowtime_Id(showtime.getId())) {
                continue;
            }
            seatRepository.deleteAll(
                    seatRepository.findAllByShowtime_IdOrderBySeatRowAscSeatNumberAsc(showtime.getId())
            );
            showtimeRepository.delete(showtime);
        }
    }

    private Long match(Map<String, Long> moviesByTitle, String cinemaTitle) {
        String normalized = normalize(cinemaTitle);
        if (normalized.isBlank()) {
            return null;
        }
        Long exact = moviesByTitle.get(normalized);
        if (exact != null) {
            return exact;
        }
        Long matched = null;
        int bestLength = 0;
        for (Map.Entry<String, Long> entry : moviesByTitle.entrySet()) {
            String title = entry.getKey();
            if (title.length() < 4 || normalized.length() < 4) {
                continue;
            }
            if (normalized.startsWith(title) || title.startsWith(normalized)) {
                int length = Math.min(title.length(), normalized.length());
                if (length > bestLength) {
                    bestLength = length;
                    matched = entry.getValue();
                }
            }
        }
        return matched;
    }

    private String normalize(String title) {
        if (title == null) {
            return "";
        }
        return title.toLowerCase(Locale.KOREAN).replaceAll("[\\s\\-:~·.!?,'\"()\\[\\]]", "");
    }

    private LocalDateTime parseDateTime(String playDate, String time) {
        if (playDate == null || playDate.isBlank() || time == null || time.isBlank()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(playDate, PLAY_DATE);
            LocalTime localTime = time.contains(":")
                    ? LocalTime.parse(time)
                    : LocalTime.parse(time, DateTimeFormatter.ofPattern("HHmm"));
            return LocalDateTime.of(date, localTime);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private int screenNumber(String screenName) {
        if (screenName == null) {
            return 1;
        }
        String digits = screenName.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return 1;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private List<Seat> createSeats(Showtime showtime, Integer totalSeats) {
        if (totalSeats == null || totalSeats <= 0) {
            return createFixedGrid(showtime);
        }
        int count = Math.min(totalSeats, MAX_SEATS);
        int perRow = count <= MAX_ROWS * SEATS_PER_ROW
                ? Math.min(SEATS_PER_ROW, count)
                : (int) Math.ceil(count / (double) MAX_ROWS);
        List<Seat> seats = new ArrayList<>();
        int placed = 0;
        int rowIndex = 0;
        while (placed < count && rowIndex < MAX_ROWS) {
            String row = String.valueOf((char) ('A' + rowIndex));
            int inRow = Math.min(perRow, count - placed);
            for (int number = 1; number <= inRow; number++) {
                seats.add(Seat.builder()
                        .showtime(showtime)
                        .seatRow(row)
                        .seatNumber(number)
                        .seatType("STANDARD")
                        .build());
            }
            placed += inRow;
            rowIndex++;
        }
        return seats;
    }

    private List<Seat> createFixedGrid(Showtime showtime) {
        List<Seat> seats = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D", "E", "F"};
        for (String row : rows) {
            for (int number = 1; number <= 8; number++) {
                seats.add(Seat.builder()
                        .showtime(showtime)
                        .seatRow(row)
                        .seatNumber(number)
                        .seatType(row.equals("E") || row.equals("F") ? "PREMIUM" : "STANDARD")
                        .build());
            }
        }
        return seats;
    }

    private void markReferenceSeats(List<Seat> seats, String sourceKey, Integer totalSeats, Integer remainingSeats) {
        if (seats.isEmpty() || totalSeats == null || remainingSeats == null || totalSeats <= 0) {
            return;
        }
        int taken = Math.max(0, totalSeats - Math.max(0, remainingSeats));
        int occupied = (int) Math.round(seats.size() * (taken / (double) totalSeats));
        occupied = Math.min(seats.size(), Math.max(0, occupied));
        List<Seat> shuffled = new ArrayList<>(seats);
        java.util.Collections.shuffle(shuffled, new java.util.Random(sourceKey.hashCode()));
        for (int i = 0; i < occupied; i++) {
            shuffled.get(i).markReferenceOccupied();
        }
    }
}
