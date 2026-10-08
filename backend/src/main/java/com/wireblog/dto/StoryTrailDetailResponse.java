package com.wireblog.dto;

import java.time.Instant;
import java.util.List;

public record StoryTrailDetailResponse(
        Long id,
        String title,
        String slug,
        String description,
        long storyCount,
        Instant updatedAt,
        List<PostSummaryResponse> stories,
        List<GroupPostTrailResponse> communityPosts
) {}
