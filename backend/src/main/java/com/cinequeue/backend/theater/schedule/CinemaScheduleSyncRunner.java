package com.cinequeue.backend.theater.schedule;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class CinemaScheduleSyncRunner implements ApplicationRunner {

    private final CinemaScheduleSyncService cinemaScheduleSyncService;

    public CinemaScheduleSyncRunner(CinemaScheduleSyncService cinemaScheduleSyncService) {
        this.cinemaScheduleSyncService = cinemaScheduleSyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread.startVirtualThread(cinemaScheduleSyncService::syncAllChainTheaters);
    }
}
