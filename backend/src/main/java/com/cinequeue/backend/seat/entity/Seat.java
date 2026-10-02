
package com.cinequeue.backend.seat.entity;

import com.cinequeue.backend.showtime.entity.Showtime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "seats",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_seat_showtime_position",
            columnNames = {"showtime_id", "seat_row", "seat_number"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which showtime this seat belongs to
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    // Seat row, such as A, B, or C
    @Column(name = "seat_row", nullable = false, length = 5)
    private String seatRow;

    // Seat number within the row
    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    // Seat category, such as STANDARD or PREMIUM
    @Column(name = "seat_type", nullable = false, length = 20)
    private String seatType;

    // Current reservation status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Builder
    public Seat(
            Showtime showtime,
            String seatRow,
            Integer seatNumber,
            String seatType
    ) {
        this.showtime = showtime;
        this.seatRow = seatRow;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.status = SeatStatus.AVAILABLE;
    }

    public void markReferenceOccupied() {
        this.status = SeatStatus.RESERVED;
    }

    public void hold() {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("Seat is not available");
        }

        this.status = SeatStatus.HOLD;
    }

    public void reserve() {
        if (this.status != SeatStatus.HOLD) {
            throw new IllegalStateException("Seat is not held");
        }

        this.status = SeatStatus.RESERVED;
    }

    // Release this seat after booking cancellation
    public void release() {
        this.status = SeatStatus.AVAILABLE;
    }
}