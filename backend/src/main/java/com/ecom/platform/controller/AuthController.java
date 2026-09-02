package com.ecom.platform.controller;

import com.ecom.platform.dto.AuthRequest;
import com.ecom.platform.dto.AuthResponse;
import com.ecom.platform.dto.RegisterRequest;
import com.ecom.platform.dto.UserDto;
import com.ecom.platform.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> request) {
        authService.verifyEmail(request.get("token"));
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        authService.forgotPassword(request.get("email"));
        return ResponseEntity.ok(Map.of("message", "Password reset email sent if account exists"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        authService.resetPassword(request.get("token"), request.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}
