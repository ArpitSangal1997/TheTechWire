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
        Set<String> tags,
        String status,
        AuthorResponse author,
        long viewCount,
        long shareCount,
        long commentCount,
        Instant publishedAt,
        Instant updatedAt
) {}
