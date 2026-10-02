package com.cinequeue.backend.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BookingRequest(
        @NotNull Long showtimeId,
        Long seatId,
        @Size(max = 10, message = "좌석은 한 번에 10개까지 선택할 수 있습니다.")
        List<Long> seatIds
) {}
