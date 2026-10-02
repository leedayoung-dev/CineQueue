package com.cinequeue.backend.user.entity;

public enum MemberGrade {
    BASIC,
    SILVER,
    GOLD,
    VIP;

    public static MemberGrade fromConfirmedCount(int count) {
        if (count >= 20) {
            return VIP;
        }
        if (count >= 6) {
            return GOLD;
        }
        if (count >= 3) {
            return SILVER;
        }
        return BASIC;
    }

    public Integer bookingsUntilNext(int count) {
        return switch (this) {
            case BASIC -> 3 - count;
            case SILVER -> 6 - count;
            case GOLD -> 20 - count;
            case VIP -> null;
        };
    }
}
