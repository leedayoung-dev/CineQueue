package com.cinequeue.backend.coupon.dto;

import com.cinequeue.backend.coupon.entity.Coupon;

import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String couponType,
        Integer discountAmount,
        String status,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCouponType().name(),
                coupon.getDiscountAmount(),
                coupon.getStatus().name(),
                coupon.getIssuedAt(),
                coupon.getExpiresAt()
        );
    }
}
