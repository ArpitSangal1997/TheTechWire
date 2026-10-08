package com.wireblog.dto;

import java.time.Instant;

public record NewsHeadlineResponse(
        Long id,
        String title,
        String sourceUrl,
        String sourceName,
        String imageUrl,
        String description,
        String category,
        Instant publishedAt
) {}
