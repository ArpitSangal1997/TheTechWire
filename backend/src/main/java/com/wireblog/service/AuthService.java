package com.wireblog.service;

import com.wireblog.config.JwtUtil;
import com.wireblog.dto.AuthResponse;
import com.wireblog.dto.LoginRequest;
import com.wireblog.dto.ProfileUpdateRequest;
import com.wireblog.dto.RegisterRequest;
import com.wireblog.exception.ApiException;
import com.wireblog.model.Role;
import com.wireblog.model.User;
import com.wireblog.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CurrentUserResolver currentUserResolver;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       CurrentUserResolver currentUserResolver) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        String handle = req.handle().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("An account with this email already exists.");
        }
        if (userRepository.existsByHandle(handle)) {
            throw ApiException.conflict("That handle is taken — try another.");
        }

        User user = User.builder()
                .displayName(req.displayName())
                .handle(handle)
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .avatarUrl(req.avatarUrl())
                .role(Role.AUTHOR) // anyone who signs up can write; promote to ADMIN manually
                .emailVerified(true)
                .build();

        user = userRepository.save(user);
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


    public AuthResponse me() {
        return toAuthResponse(currentUserResolver.requireCurrentUser());
    }

    public AuthResponse updateProfile(ProfileUpdateRequest request) {
        User user = currentUserResolver.requireCurrentUser();
        user.setDisplayName(request.displayName().trim());
        user.setBio(clean(request.bio()));
        user.setAvatarUrl(clean(request.avatarUrl()));
        return toAuthResponse(userRepository.save(user));
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getDisplayName(), user.getHandle(),
                user.getAvatarUrl(), user.getBio(), user.getRole().name(), user.isEmailVerified());
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
