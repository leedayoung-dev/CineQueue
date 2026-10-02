package com.cinequeue.backend.booking.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HoldExpirationScheduler {

    private final BookingService bookingService;

    public HoldExpirationScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelay = 1000)
    public void expireHolds() {
        bookingService.expireHolds();
    }
}
