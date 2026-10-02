package com.cinequeue.backend.showtime;

import com.cinequeue.backend.user.entity.AgeGroup;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class TicketPrice {

    private static final LocalTime EARLY_UNTIL = LocalTime.of(10, 0);

    private TicketPrice() {
    }

    public static int forAge(LocalDateTime startTime, AgeGroup ageGroup) {
        AgeGroup group = ageGroup == null ? AgeGroup.ADULT : ageGroup;
        boolean weekend = startTime != null && startTime.getDayOfWeek().getValue() >= DayOfWeek.FRIDAY.getValue();
        boolean early = startTime != null && startTime.toLocalTime().isBefore(EARLY_UNTIL);

        if (group == AgeGroup.CHILD) {
            return 6000;
        }
        if (group == AgeGroup.TEEN) {
            if (early) {
                return weekend ? 9000 : 8000;
            }
            return weekend ? 13000 : 12000;
        }
        if (early) {
            return weekend ? 11000 : 10000;
        }
        return weekend ? 15000 : 14000;
    }
}
