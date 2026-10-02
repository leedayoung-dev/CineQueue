package com.cinequeue.backend.theater.service;

import com.cinequeue.backend.showtime.entity.Showtime;
import com.cinequeue.backend.showtime.repository.ShowtimeRepository;
import com.cinequeue.backend.theater.entity.Theater;
import com.cinequeue.backend.theater.repository.TheaterRepository;
import com.cinequeue.backend.user.entity.DistrictCatalog;
import com.cinequeue.backend.user.entity.Region;
import com.cinequeue.backend.user.entity.User;
import com.cinequeue.backend.user.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class TheaterDataInitializer implements ApplicationRunner {

    private final TheaterRepository theaterRepository;
    private final ShowtimeRepository showtimeRepository;
    private final UserRepository userRepository;

    public TheaterDataInitializer(
            TheaterRepository theaterRepository,
            ShowtimeRepository showtimeRepository,
            UserRepository userRepository
    ) {
        this.theaterRepository = theaterRepository;
        this.showtimeRepository = showtimeRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureTheaters();
        renameLegacyDistricts();
        linkShowtimes();
    }

    @Transactional
    public List<Theater> ensureTheaters() {
        if (theaterRepository.count() == 0) {
            theaterRepository.saveAll(List.of(
                    theater("CGV 용산아이파크몰", Region.SEOUL, "용산구", "서울 용산구 한강대로23길 55", 37.5299, 126.9648),
                    theater("롯데시네마 건대입구", Region.SEOUL, "광진구", "서울 광진구 아차산로 272", 37.5404, 127.0706),
                    theater("메가박스 코엑스", Region.SEOUL, "강남구", "서울 강남구 봉은사로 524", 37.5126, 127.0588),
                    theater("CGV 왕십리", Region.SEOUL, "성동구", "서울 성동구 왕십리로 50", 37.5614, 127.0380),
                    theater("CGV 수원", Region.GYEONGGI, "수원시 팔달구", "경기 수원시 팔달구 덕영대로 924", 37.2659, 127.0002),
                    theater("메가박스 분당", Region.GYEONGGI, "성남시 분당구", "경기 성남시 분당구 황새울로 360", 37.3850, 127.1230),
                    theater("롯데시네마 평촌", Region.GYEONGGI, "안양시 동안구", "경기 안양시 동안구 관평로 210", 37.3896, 126.9510),
                    theater("CGV 인천", Region.INCHEON, "남동구", "인천 남동구 예술로 198", 37.4420, 126.7010),
                    theater("메가박스 송도", Region.INCHEON, "연수구", "인천 연수구 송도과학로 16번길 33", 37.3826, 126.6560),
                    theater("CGV 춘천", Region.GANGWON, "춘천시", "강원 춘천시 중앙로 1", 37.8813, 127.7298),
                    theater("롯데시네마 대전", Region.CHUNGCHEONG, "대전 서구", "대전 서구 계룡로 598", 36.3510, 127.3780),
                    theater("CGV 광주", Region.JEOLLA, "광주 동구", "광주 동구 중앙로 160", 35.1490, 126.9150),
                    theater("메가박스 해운대", Region.GYEONGSANG, "부산 해운대구", "부산 해운대구 센텀서로 20", 35.1690, 129.1310),
                    theater("CGV 제주", Region.JEJU, "제주시", "제주 제주시 중앙로 1", 33.4996, 126.5312),
                    theater("CineQueue 시네마", Region.OTHER, "기타", "기타 지역", 37.5665, 126.9780)
            ));
        }
        return theaterRepository.findAllByOrderByNameAsc();
    }

    private void renameLegacyDistricts() {
        for (Theater theater : theaterRepository.findAll()) {
            String next = DistrictCatalog.legacyName(theater.getRegion(), theater.getDistrict());
            if (next != null) {
                theater.renameDistrict(next);
            }
        }
        for (User user : userRepository.findAll()) {
            String next = DistrictCatalog.legacyName(user.getRegion(), user.getDistrict());
            if (next != null) {
                user.renameDistrict(next);
            }
        }
    }

    private void linkShowtimes() {
        List<Theater> theaters = theaterRepository.findAllByOrderByNameAsc();
        if (theaters.isEmpty()) {
            return;
        }
        List<Showtime> unlinked = showtimeRepository.findAllByTheaterIsNull();
        for (int i = 0; i < unlinked.size(); i++) {
            unlinked.get(i).assignTheater(theaters.get(i % theaters.size()));
        }
    }

    private Theater theater(
            String name,
            Region region,
            String district,
            String address,
            double latitude,
            double longitude
    ) {
        return new Theater(name, region, district, address, latitude, longitude);
    }
}
