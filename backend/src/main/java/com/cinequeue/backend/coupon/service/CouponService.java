package com.cinequeue.backend.coupon.service;

import com.cinequeue.backend.booking.entity.Booking;
import com.cinequeue.backend.coupon.dto.CouponResponse;
import com.cinequeue.backend.coupon.entity.Coupon;
import com.cinequeue.backend.coupon.entity.CouponStatus;
import com.cinequeue.backend.coupon.entity.CouponType;
import com.cinequeue.backend.coupon.repository.CouponRepository;
import com.cinequeue.backend.user.entity.MemberGrade;
import com.cinequeue.backend.user.entity.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional
    public void issueWelcome(User user) {
        issueOnce(user, CouponType.WELCOME);
    }

    @Transactional
    public void issueGradeReward(User user, MemberGrade grade) {
        CouponType couponType = switch (grade) {
            case SILVER -> CouponType.SILVER_REWARD;
            case GOLD -> CouponType.GOLD_REWARD;
            case VIP -> CouponType.VIP_REWARD;
            case BASIC -> null;
        };
        if (couponType != null) {
            issueOnce(user, couponType);
        }
    }

    @Transactional
    public List<CouponResponse> listMine(User user) {
        issueWelcome(user);
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findAllByUser_IdOrderByIssuedAtDesc(user.getId())
                .stream()
                .peek(coupon -> {
                    if (coupon.getStatus() == CouponStatus.AVAILABLE && !coupon.getExpiresAt().isAfter(now)) {
                        coupon.expire();
                    }
                })
                .map(CouponResponse::from)
                .toList();
    }

    @Transactional
    public void apply(User user, Long couponId, Booking booking) {
        Coupon coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        if (!coupon.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인의 쿠폰만 사용할 수 있습니다.");
        }

        int price = booking.getPaidPrice() == null ? 0 : booking.getPaidPrice();
        int discount = Math.min(coupon.getDiscountAmount(), price);
        booking.applyDiscount(discount);
        coupon.use(booking, LocalDateTime.now());
    }

    private void issueOnce(User user, CouponType couponType) {
        if (couponRepository.existsByUser_IdAndCouponType(user.getId(), couponType)) {
            return;
        }
        try {
            couponRepository.saveAndFlush(new Coupon(user, couponType, LocalDateTime.now()));
        } catch (DataIntegrityViolationException ignored) {
            // 같은 쿠폰이 동시에 발급된 경우 한 장만 유지한다.
        }
    }
}
