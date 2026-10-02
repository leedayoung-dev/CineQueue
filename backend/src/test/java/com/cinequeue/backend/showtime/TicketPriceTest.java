package com.cinequeue.backend.showtime;

import com.cinequeue.backend.user.entity.AgeGroup;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TicketPriceTest {

    @Test
    void weekdayMorningAndRegularPrices() {
        LocalDateTime morning = LocalDateTime.of(2026, 10, 1, 9, 50);
        LocalDateTime regular = LocalDateTime.of(2026, 10, 1, 10, 0);

        assertEquals(10000, TicketPrice.forAge(morning, AgeGroup.ADULT));
        assertEquals(8000, TicketPrice.forAge(morning, AgeGroup.TEEN));
        assertEquals(6000, TicketPrice.forAge(morning, AgeGroup.CHILD));
        assertEquals(14000, TicketPrice.forAge(regular, AgeGroup.ADULT));
        assertEquals(12000, TicketPrice.forAge(regular, AgeGroup.TEEN));
        assertEquals(6000, TicketPrice.forAge(regular, AgeGroup.CHILD));
    }

    @Test
    void weekendPricesIncludeFriday() {
        LocalDateTime fridayMorning = LocalDateTime.of(2026, 10, 2, 9, 0);
        LocalDateTime sundayRegular = LocalDateTime.of(2026, 10, 4, 18, 30);

        assertEquals(11000, TicketPrice.forAge(fridayMorning, AgeGroup.ADULT));
        assertEquals(9000, TicketPrice.forAge(fridayMorning, AgeGroup.TEEN));
        assertEquals(15000, TicketPrice.forAge(sundayRegular, AgeGroup.ADULT));
        assertEquals(13000, TicketPrice.forAge(sundayRegular, AgeGroup.TEEN));
        assertEquals(6000, TicketPrice.forAge(sundayRegular, AgeGroup.CHILD));
    }
}
