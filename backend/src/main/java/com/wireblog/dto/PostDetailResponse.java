package com.wireblog.dto;

import java.time.Instant;
import java.util.Set;

public record PostDetailResponse(
        Long id,
        String title,
        String slug,
        String excerpt,
        String content,
        String coverImageUrl,
        String sourceUrl,
        Set<String> tags,
        String status,
        AuthorResponse author,
        long viewCount,
        long shareCount,
        long commentCount,
        StoryTrailRefResponse trail,
        Instant publishedAt,
        Instant updatedAt
) {}
