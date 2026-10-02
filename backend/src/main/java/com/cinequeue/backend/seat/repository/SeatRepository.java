
package com.cinequeue.backend.seat.repository;

import com.cinequeue.backend.seat.entity.Seat;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // 상영 회차의 좌석을 행과 좌석 번호 순서로 조회
    List<Seat> findAllByShowtime_IdOrderBySeatRowAscSeatNumberAsc(
            Long showtimeId
    );

    // 예매 시 특정 좌석에 비관적 쓰기 잠금 적용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :seatId")
    Optional<Seat> findByIdForUpdate(
            @Param("seatId") Long seatId
    );
}