package com.rawqubit.jwtauth.controller;

import com.rawqubit.jwtauth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Data
    public static class RegisterDto {
        @NotBlank private String username;
        @Email @NotBlank private String email;
        @Size(min = 8, max = 128) @NotBlank private String password;
    }

    @Data
    public static class LoginDto {
        @Email @NotBlank private String email;
        @NotBlank private String password;
    }

    @Data
    public static class RefreshDto {
        @NotBlank private String refreshToken;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthService.AuthResponse> register(@Valid @RequestBody RegisterDto dto) {
        return ResponseEntity.ok(authService.register(
                new AuthService.RegisterRequest(dto.getUsername(), dto.getEmail(), dto.getPassword())));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthService.AuthResponse> login(@Valid @RequestBody LoginDto dto) {
        return ResponseEntity.ok(authService.login(
                new AuthService.LoginRequest(dto.getEmail(), dto.getPassword())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthService.AuthResponse> refresh(@Valid @RequestBody RefreshDto dto) {
        return ResponseEntity.ok(authService.refresh(dto.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshDto dto) {
        authService.logout(dto.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
