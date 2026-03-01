package com.rawqubit.jwtauth.service;

import com.rawqubit.jwtauth.model.RefreshToken;
import com.rawqubit.jwtauth.model.Role;
import com.rawqubit.jwtauth.model.User;
import com.rawqubit.jwtauth.repository.RefreshTokenRepository;
import com.rawqubit.jwtauth.repository.UserRepository;
import com.rawqubit.jwtauth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.refresh-token-expiry-ms:604800000}") // 7 days default
    private long refreshTokenExpiryMs;

    public record RegisterRequest(String username, String email, String password) {}
    public record LoginRequest(String email, String password) {}
    public record AuthResponse(String accessToken, String refreshToken, String userId, String email) {}

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(Role.ROLE_USER))
                .build();

        userRepository.save(user);
        log.info("Registered new user: {}", user.getEmail());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getEmail());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);
        log.info("User logged in: {}", user.getEmail());
        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getEmail());
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(rawRefreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (token.isRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new IllegalArgumentException("Refresh token has expired — please log in again");
        }

        // Rotate: revoke old, issue new
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        User user = token.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = createRefreshToken(user);
        return new AuthResponse(newAccessToken, newRefreshToken, user.getId(), user.getEmail());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByToken(rawRefreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private String createRefreshToken(User user) {
        // Revoke any existing active tokens for this user
        refreshTokenRepository.revokeAllByUser(user);

        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiryMs))
                .build();
        refreshTokenRepository.save(token);
        return token.getToken();
    }
}
