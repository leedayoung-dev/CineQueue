
package com.cinequeue.backend.user.controller;

import com.cinequeue.backend.coupon.dto.CouponResponse;
import com.cinequeue.backend.user.dto.LoginRequest;
import com.cinequeue.backend.user.dto.LoginResponse;
import com.cinequeue.backend.user.dto.SignupRequest;
import com.cinequeue.backend.user.dto.SignupResponse;
import com.cinequeue.backend.user.dto.UpdateProfileRequest;
import com.cinequeue.backend.user.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 회원가입 API
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        SignupResponse response = userService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 로그인 API
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = userService.login(request);

        return ResponseEntity.ok(response);
    }

    // 로그인한 사용자 정보 확인 API
    @GetMapping("/me")
    public ResponseEntity<SignupResponse> me(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                userService.currentUser(authentication.getName())
        );
    }

    @GetMapping("/me/coupons")
    public ResponseEntity<List<CouponResponse>> myCoupons(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                userService.myCoupons(authentication.getName())
        );
    }

    // 내 프로필 수정 API
    @PatchMapping("/me")
    public ResponseEntity<SignupResponse> updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(
                userService.updateProfile(authentication.getName(), request)
        );
    }
}