package com.cinequeue.backend.theater.service;

import com.cinequeue.backend.showtime.dto.ShowtimeResponse;
import com.cinequeue.backend.showtime.repository.ShowtimeRepository;
import com.cinequeue.backend.theater.dto.TheaterResponse;
import com.cinequeue.backend.theater.entity.Theater;
import com.cinequeue.backend.theater.repository.TheaterRepository;
import com.cinequeue.backend.user.entity.Region;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TheaterService {

    private final TheaterRepository theaterRepository;
    private final ShowtimeRepository showtimeRepository;

    public TheaterService(
            TheaterRepository theaterRepository,
            ShowtimeRepository showtimeRepository
    ) {
        this.theaterRepository = theaterRepository;
        this.showtimeRepository = showtimeRepository;
    }

    public List<TheaterResponse> nearby(Region region, String district) {
        List<Theater> theaters = theaterRepository.findAll();
        double[] origin = origin(theaters, region, district);
        return theaters.stream()
                .map(theater -> toResponse(theater, origin))
                .sorted(Comparator.comparing(TheaterResponse::distanceKm))
                .toList();
    }

    public List<ShowtimeResponse> upcomingShowtimes(Long theaterId) {
        if (!theaterRepository.existsById(theaterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "영화관을 찾을 수 없습니다.");
        }
        return showtimeRepository
                .findAllByTheater_IdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
                        theaterId,
                        LocalDateTime.now()
                )
                .stream()
                .map(ShowtimeResponse::from)
                .toList();
    }

    private TheaterResponse toResponse(Theater theater, double[] origin) {
        double distance = distanceKm(origin[0], origin[1], theater.getLatitude(), theater.getLongitude());
        return new TheaterResponse(
                theater.getId(),
                theater.getName(),
                theater.getRegion().name(),
                theater.getDistrict(),
                theater.getAddress(),
                theater.getLatitude(),
                theater.getLongitude(),
                Math.round(distance * 10.0) / 10.0
        );
    }

    private double[] origin(List<Theater> theaters, Region region, String district) {
        if (district != null && !district.isBlank()) {
            List<Theater> sameDistrict = theaters.stream()
                    .filter(theater -> theater.getDistrict().equals(district))
                    .toList();
            if (!sameDistrict.isEmpty()) {
                return average(sameDistrict);
            }
        }
        if (region != null) {
            List<Theater> sameRegion = theaters.stream()
                    .filter(theater -> theater.getRegion() == region)
                    .toList();
            if (!sameRegion.isEmpty()) {
                return average(sameRegion);
            }
        }
        return new double[]{37.5665, 126.9780};
    }

    private double[] average(List<Theater> theaters) {
        double latitude = theaters.stream().mapToDouble(Theater::getLatitude).average().orElse(37.5665);
        double longitude = theaters.stream().mapToDouble(Theater::getLongitude).average().orElse(126.9780);
        return new double[]{latitude, longitude};
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double lat = Math.toRadians(lat2 - lat1);
        double lon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(lat / 2) * Math.sin(lat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lon / 2) * Math.sin(lon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
