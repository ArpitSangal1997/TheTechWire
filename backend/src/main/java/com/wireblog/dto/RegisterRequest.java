package com.wireblog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String displayName,
        @NotBlank @Size(min = 3, max = 30) String handle,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        String avatarUrl
) {}
