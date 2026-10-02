package com.cinequeue.backend.theater.dto;

public record TheaterResponse(
        Long id,
        String name,
        String region,
        String district,
        String address,
        Double latitude,
        Double longitude,
        Double distanceKm
) {
}
