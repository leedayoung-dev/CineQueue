package com.cinequeue.backend.coupon.entity;

import com.cinequeue.backend.booking.entity.Booking;
import com.cinequeue.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "coupons",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_coupon_user_type",
                columnNames = {"user_id", "coupon_type"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", nullable = false, length = 30)
    private CouponType couponType;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    public Coupon(User user, CouponType couponType, LocalDateTime issuedAt) {
        this.user = user;
        this.couponType = couponType;
        this.discountAmount = couponType.discountAmount();
        this.status = CouponStatus.AVAILABLE;
        this.issuedAt = issuedAt;
        this.expiresAt = issuedAt.plusDays(30);
    }

    public void expire() {
        if (this.status == CouponStatus.AVAILABLE) {
            this.status = CouponStatus.EXPIRED;
        }
    }

    public void use(Booking booking, LocalDateTime usedAt) {
        if (this.status != CouponStatus.AVAILABLE) {
            throw new IllegalStateException("사용할 수 있는 쿠폰이 아닙니다.");
        }
        if (!this.expiresAt.isAfter(usedAt)) {
            throw new IllegalStateException("만료된 쿠폰입니다.");
        }
        this.status = CouponStatus.USED;
        this.usedAt = usedAt;
        this.booking = booking;
    }
}
