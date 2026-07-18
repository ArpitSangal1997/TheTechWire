package com.wireblog.controller;

import com.wireblog.dto.AuthResponse;
import com.wireblog.dto.LoginRequest;
import com.wireblog.dto.RegisterRequest;
import com.wireblog.dto.PasswordResetConfirmRequest;
import com.wireblog.dto.PasswordResetRequest;
import com.wireblog.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/verify-email")
    public void verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
    }

    @PostMapping("/password-reset/request")
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest req) {
        authService.requestPasswordReset(req.email());
    }

    @PostMapping("/password-reset/confirm")
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest req) {
        authService.confirmPasswordReset(req.token(), req.newPassword());
    }
}
