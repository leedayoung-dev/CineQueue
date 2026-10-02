package com.cinequeue.backend.theater.entity;

import com.cinequeue.backend.user.entity.Region;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "theaters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Theater {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Region region;

    @Column(nullable = false, length = 30)
    private String district;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CinemaChain chain;

    public Theater(
            String name,
            Region region,
            String district,
            String address,
            double latitude,
            double longitude
    ) {
        this.name = name;
        this.region = region;
        this.district = district;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void renameDistrict(String district) {
        this.district = district;
    }

    public void assignChain(CinemaChain chain) {
        this.chain = chain;
    }
}
