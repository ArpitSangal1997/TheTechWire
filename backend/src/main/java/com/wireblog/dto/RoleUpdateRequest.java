package com.wireblog.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleUpdateRequest(@NotBlank String role) {}
