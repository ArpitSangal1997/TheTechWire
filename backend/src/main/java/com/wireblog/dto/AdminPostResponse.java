package com.wireblog.dto;

import java.time.Instant;

public record AdminPostResponse(
        Long id,
        String title,
        String slug,
        String status,
        AuthorResponse author,
        StoryTrailRefResponse trail,
        long viewCount,
        long shareCount,
        long commentCount,
        Instant createdAt,
        Instant publishedAt,
        Instant updatedAt
) {}
