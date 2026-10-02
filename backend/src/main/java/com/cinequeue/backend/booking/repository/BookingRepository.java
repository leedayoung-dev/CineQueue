
package com.cinequeue.backend.booking.repository;

import com.cinequeue.backend.booking.entity.Booking;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cinequeue.backend.booking.entity.BookingStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByUser_IdAndStatusInOrderByBookedAtDesc(
            Long userId,
            Collection<BookingStatus> statuses
    );

    List<Booking> findAllByUser_IdAndStatus(Long userId, BookingStatus status);

    long countByUser_IdAndStatus(Long userId, BookingStatus status);

    boolean existsByShowtime_Id(Long showtimeId);

    @Query("""
            SELECT b.id FROM Booking b
            WHERE b.status = :status
              AND b.expiresAt IS NOT NULL
              AND b.expiresAt < :now
            """)
    List<Long> findExpiredHoldIds(
            @Param("status") BookingStatus status,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :bookingId")
    Optional<Booking> findByIdForUpdate(
        @Param("bookingId") Long bookingId
    );
}