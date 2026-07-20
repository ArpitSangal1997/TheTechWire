package com.wireblog.dto;

import java.time.Instant;

public record AdminUserResponse(Long id, String displayName, String handle, String email,
                                String role, boolean enabled, Instant createdAt) {}
