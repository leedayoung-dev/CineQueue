package com.cinequeue.backend.coupon.entity;

public enum CouponType {
    WELCOME(1000),
    SILVER_REWARD(2000),
    GOLD_REWARD(3000),
    VIP_REWARD(5000);

    private final int discountAmount;

    CouponType(int discountAmount) {
        this.discountAmount = discountAmount;
    }

    public int discountAmount() {
        return discountAmount;
    }
}
