package com.wireblog.dto;

public record AuthResponse(
        String token,
        Long userId,
        String displayName,
        String handle,
        String avatarUrl,
        String role,
        boolean emailVerified
) {}
