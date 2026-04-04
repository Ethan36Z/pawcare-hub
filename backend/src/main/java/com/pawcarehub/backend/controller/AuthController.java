package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.auth.AuthResponse;
import com.pawcarehub.backend.dto.auth.ChangePasswordRequest;
import com.pawcarehub.backend.dto.auth.LoginRequest;
import com.pawcarehub.backend.dto.auth.RegisterRequest;
import com.pawcarehub.backend.dto.auth.UpdateUserProfileRequest;
import com.pawcarehub.backend.dto.auth.UserProfileResponse;
import com.pawcarehub.backend.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public UserProfileResponse getProfile(
        @RequestHeader("X-User-Email") String userEmailHeader
    ) {
        return authService.getProfile(userEmailHeader);
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @RequestBody UpdateUserProfileRequest request
    ) {
        UserProfileResponse response = authService.updateProfile(userEmailHeader, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/change-password")
    public ResponseEntity<Void> changePassword(
        @RequestHeader("X-User-Email") String userEmailHeader,
        @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(userEmailHeader, request);
        return ResponseEntity.noContent().build();
    }
}
