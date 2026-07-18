package com.wireblog.dto;

import java.time.LocalDate;

public record SponsorshipResponse(
        Long id,
        String brandName,
        String headline,
        String targetUrl,
        String creativeImageUrl,
        String placement,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        long impressions,
        long clicks
) {}
