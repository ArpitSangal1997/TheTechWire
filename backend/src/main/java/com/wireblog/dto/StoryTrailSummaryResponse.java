package com.wireblog.dto;

public record StoryTrailSummaryResponse(
        Long id,
        String title,
        String slug,
        String description,
        long storyCount
) {}
