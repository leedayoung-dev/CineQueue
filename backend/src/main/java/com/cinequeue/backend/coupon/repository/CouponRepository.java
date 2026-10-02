package com.cinequeue.backend.coupon.repository;

import com.cinequeue.backend.coupon.entity.Coupon;
import com.cinequeue.backend.coupon.entity.CouponType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    boolean existsByUser_IdAndCouponType(Long userId, CouponType couponType);

    List<Coupon> findAllByUser_IdOrderByIssuedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :couponId")
    Optional<Coupon> findByIdForUpdate(@Param("couponId") Long couponId);
}
