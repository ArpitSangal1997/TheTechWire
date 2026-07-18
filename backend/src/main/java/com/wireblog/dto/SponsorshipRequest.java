package com.wireblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SponsorshipRequest(
        @NotBlank String brandName,
        @NotBlank String headline,
        @NotBlank String targetUrl,
        String creativeImageUrl,
        @NotNull String placement,
        @NotNull String status,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {}
