package com.wireblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 500) String bio,
        String avatarUrl
) {}
