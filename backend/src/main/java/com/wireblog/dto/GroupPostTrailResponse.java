package com.wireblog.dto;

import java.time.Instant;

public record GroupPostTrailResponse(
        Long id,
        Long groupId,
        String groupName,
        String title,
        String body,
        Long authorId,
        String authorName,
        String authorHandle,
        Instant createdAt,
        long commentCount,
        boolean canOpenGroup
) {}
