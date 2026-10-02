
package com.cinequeue.backend.user.service;

import com.cinequeue.backend.coupon.dto.CouponResponse;
import com.cinequeue.backend.security.JwtTokenProvider;
import com.cinequeue.backend.user.dto.LoginRequest;
import com.cinequeue.backend.user.dto.LoginResponse;
import com.cinequeue.backend.user.dto.SignupRequest;
import com.cinequeue.backend.user.dto.SignupResponse;
import com.cinequeue.backend.user.dto.UpdateProfileRequest;
import com.cinequeue.backend.coupon.service.CouponService;
import com.cinequeue.backend.user.entity.DistrictCatalog;
import com.cinequeue.backend.user.entity.MemberGrade;
import com.cinequeue.backend.user.entity.User;
import com.cinequeue.backend.user.repository.UserRepository;
import com.cinequeue.backend.booking.entity.BookingStatus;
import com.cinequeue.backend.booking.repository.BookingRepository;

import jakarta.persistence.EntityManager;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CouponService couponService;
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(
            UserRepository userRepository,
            BookingRepository bookingRepository,
            CouponService couponService,
            EntityManager entityManager,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.couponService = couponService;
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 회원가입
    @Transactional
    public SignupResponse signup(SignupRequest request) {

        if (!DistrictCatalog.contains(request.region(), request.district())) {
            throw new IllegalArgumentException("선택한 지역의 시·군·구가 아닙니다.");
        }

        // 1. 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 이메일입니다."
            );
        }

        // 2. 비밀번호 암호화
        String encodedPassword =
                passwordEncoder.encode(request.password());

        // 3. 요청 데이터를 User 엔티티로 변환
        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .name(request.name())
                .region(request.region())
                .district(request.district())
                .preferredSeat(request.preferredSeat())
                .ageGroup(request.ageGroup())
                .build();

        // 4. DB에 회원 저장
        User savedUser = userRepository.save(user);
        couponService.issueWelcome(savedUser);

        // 5. 응답 DTO 반환
        return SignupResponse.from(savedUser, 0);
    }

    // 로그인
    public LoginResponse login(LoginRequest request) {

        // 1. 이메일로 회원 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "이메일 또는 비밀번호가 올바르지 않습니다."
                ));

        // 2. 입력한 비밀번호와 저장된 해시 비교
        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPassword()
        );

        // 3. 비밀번호가 일치하지 않으면 401 반환
        if (!passwordMatches) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        // 4. 로그인 성공 시 JWT 발급
        String accessToken =
                jwtTokenProvider.createToken(user.getEmail());
        couponService.issueWelcome(user);

        // 5. JWT를 응답 DTO로 반환
        return new LoginResponse(accessToken, "Bearer");
    }

    @Transactional(readOnly = true)
    public SignupResponse currentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "로그인 사용자 정보를 찾을 수 없습니다."
                ));

        return SignupResponse.from(user, confirmedBookingCount(user));
    }

    @Transactional
    public List<CouponResponse> myCoupons(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "로그인 사용자 정보를 찾을 수 없습니다."
                ));
        return couponService.listMine(user);
    }

    // 내 프로필 수정
    @Transactional
    public SignupResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "로그인 사용자 정보를 찾을 수 없습니다."
                ));

        if (!DistrictCatalog.contains(request.region(), request.district())) {
            throw new IllegalArgumentException("선택한 지역의 시·군·구가 아닙니다.");
        }

        user.updateProfile(
                request.name().trim(),
                request.region(),
                request.preferredSeat(),
                request.ageGroup(),
                request.district()
        );

        return SignupResponse.from(user, confirmedBookingCount(user));
    }

    @Transactional
    public void refreshMembershipGrade(User user) {
        entityManager.flush();
        int count = confirmedBookingCount(user);
        MemberGrade previous = user.getMembershipGrade() == null
                ? MemberGrade.BASIC
                : user.getMembershipGrade();
        MemberGrade next = MemberGrade.fromConfirmedCount(count);
        user.updateMembershipGrade(next);
        if (next.ordinal() > previous.ordinal()) {
            couponService.issueGradeReward(user, next);
        }
    }

    private int confirmedBookingCount(User user) {
        return (int) bookingRepository.countByUser_IdAndStatus(
                user.getId(),
                BookingStatus.CONFIRMED
        );
    }
}