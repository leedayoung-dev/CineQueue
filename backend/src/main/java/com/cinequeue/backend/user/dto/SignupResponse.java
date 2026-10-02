
package com.cinequeue.backend.user.dto;

import com.cinequeue.backend.user.entity.AgeGroup;
import com.cinequeue.backend.user.entity.MemberGrade;
import com.cinequeue.backend.user.entity.PreferredSeat;
import com.cinequeue.backend.user.entity.Region;
import com.cinequeue.backend.user.entity.User;

import java.time.LocalDateTime;

public record SignupResponse(
        Long id,
        String email,
        String name,
        Region region,
        String district,
        PreferredSeat preferredSeat,
        AgeGroup ageGroup,
        LocalDateTime createdAt,
        MemberGrade membershipGrade,
        int confirmedBookingCount,
        Integer bookingsUntilNextGrade
) {
    public static SignupResponse from(User user, int confirmedBookingCount) {
        MemberGrade grade = MemberGrade.fromConfirmedCount(confirmedBookingCount);
        return new SignupResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRegion(),
                user.getDistrict(),
                user.getPreferredSeat(),
                user.getAgeGroup() == null ? AgeGroup.ADULT : user.getAgeGroup(),
                user.getCreatedAt(),
                grade,
                confirmedBookingCount,
                grade.bookingsUntilNext(confirmedBookingCount)
        );
    }
}