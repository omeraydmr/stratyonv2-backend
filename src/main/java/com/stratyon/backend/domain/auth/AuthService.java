package com.stratyon.backend.domain.auth;

import com.stratyon.backend.domain.auth.dto.AuthResponse;
import com.stratyon.backend.domain.auth.dto.LoginRequest;
import com.stratyon.backend.domain.auth.dto.RegisterRequest;
import com.stratyon.backend.shared.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email already registered.");
        }
        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .companyName(req.companyName())
                .build();
        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return toResponse(user, token);
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        User user = userRepository.findByEmail(req.email()).orElseThrow();
        String token = jwtService.generateToken(user);
        return toResponse(user, token);
    }

    @Transactional
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return;
        String token = authHeader.substring(7);
        try {
            String jti = jwtService.extractJti(token);
            OffsetDateTime exp = jwtService.extractExpiration(token).toInstant()
                    .atOffset(java.time.ZoneOffset.UTC);
            tokenBlacklistRepository.save(TokenBlacklist.builder()
                    .tokenJti(jti)
                    .expiresAt(exp)
                    .build());
        } catch (Exception ignored) {}
    }

    private AuthResponse toResponse(User user, String token) {
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getCompanyName(),
                user.getFirm() != null ? user.getFirm().getId() : null,
                token
        );
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        tokenBlacklistRepository.deleteExpiredBefore(OffsetDateTime.now());
    }
}
