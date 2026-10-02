
package com.cinequeue.backend.booking.controller;

import com.cinequeue.backend.booking.dto.PayBookingRequest;
import com.cinequeue.backend.booking.dto.BookingRequest;
import com.cinequeue.backend.booking.dto.BookingResponse;
import com.cinequeue.backend.booking.service.BookingService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * 예매 생성
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            Authentication authentication,
            @Valid @RequestBody BookingRequest request
    ) {
        BookingResponse response = bookingService.createBooking(
                authentication.getName(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{bookingId}/pay")
    public ResponseEntity<BookingResponse> payBooking(
            Authentication authentication,
            @PathVariable Long bookingId,
            @RequestBody(required = false) PayBookingRequest request
    ) {
        return ResponseEntity.ok(
                bookingService.payBooking(
                        bookingId,
                        authentication.getName(),
                        request == null ? null : request.couponId()
                )
        );
    }

    @DeleteMapping("/{bookingId}/hold")
    public ResponseEntity<BookingResponse> releaseHold(
            Authentication authentication,
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(
                bookingService.releaseHold(bookingId, authentication.getName())
        );
    }

    /**
     * 로그인한 사용자의 예매 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                bookingService.getMyBookings(
                        authentication.getName()
                )
        );
    }

    /**
     * 특정 예매 상세 조회
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getMyBooking(
            Authentication authentication,
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(
                bookingService.getBooking(
                        bookingId,
                        authentication.getName()
                )
        );
    }

    /**
     * 예매 취소
     */
    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            Authentication authentication,
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(
                bookingService.cancelBooking(
                        bookingId,
                        authentication.getName()
                )
        );
    }
}