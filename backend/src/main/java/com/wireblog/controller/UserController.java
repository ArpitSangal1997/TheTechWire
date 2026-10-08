package com.wireblog.controller;

import com.wireblog.dto.AuthResponse;
import com.wireblog.dto.ProfileUpdateRequest;
import com.wireblog.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public AuthResponse me() {
        return authService.me();
    }

    @PatchMapping("/me")
    public AuthResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return authService.updateProfile(request);
    }
}
