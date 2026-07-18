package com.wireblog.service;

import com.wireblog.config.JwtUtil;
import com.wireblog.dto.AuthResponse;
import com.wireblog.dto.LoginRequest;
import com.wireblog.dto.RegisterRequest;
import com.wireblog.exception.ApiException;
import com.wireblog.model.Role;
import com.wireblog.model.User;
import com.wireblog.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw ApiException.conflict("An account with this email already exists.");
        }
        if (userRepository.existsByHandle(req.handle())) {
            throw ApiException.conflict("That handle is taken — try another.");
        }

        String verificationToken = UUID.randomUUID().toString();
        User user = User.builder()
                .displayName(req.displayName())
                .handle(req.handle().toLowerCase())
                .email(req.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .avatarUrl(req.avatarUrl())
                .role(Role.AUTHOR) // anyone who signs up can write; promote to ADMIN manually
                .verificationToken(verificationToken)
                .build();

        user = userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> ApiException.badRequest("Incorrect email or password."));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.badRequest("Incorrect email or password.");
        }
        if (!user.isEnabled()) {
            throw ApiException.forbidden("This account has been suspended.");
        }

        return toAuthResponse(user);
    }

    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired verification link."));
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
    }

    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email.toLowerCase()).orElse(null);
        if (user == null) return;
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        userRepository.save(user);
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    public void confirmPasswordReset(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset link."));
        if (user.getPasswordResetExpiresAt() == null || user.getPasswordResetExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("Invalid or expired reset link.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getDisplayName(), user.getHandle(),
                user.getAvatarUrl(), user.getRole().name(), user.isEmailVerified());
    }
}
