
package com.cinequeue.backend.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Region region;

    @Column(length = 30)
    private String district;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PreferredSeat preferredSeat;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_grade", length = 20)
    private MemberGrade membershipGrade;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public User(
            String email,
            String password,
            String name,
            Region region,
            PreferredSeat preferredSeat,
            AgeGroup ageGroup,
            String district
    ) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.region = region;
        this.preferredSeat = preferredSeat;
        this.ageGroup = ageGroup;
        this.district = district;
        this.membershipGrade = MemberGrade.BASIC;
    }

    public void updateProfile(
            String name,
            Region region,
            PreferredSeat preferredSeat,
            AgeGroup ageGroup,
            String district
    ) {
        this.name = name;
        this.region = region;
        this.preferredSeat = preferredSeat;
        this.ageGroup = ageGroup;
        this.district = district;
    }

    public void renameDistrict(String district) {
        this.district = district;
    }

    public void updateMembershipGrade(MemberGrade membershipGrade) {
        this.membershipGrade = membershipGrade;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}