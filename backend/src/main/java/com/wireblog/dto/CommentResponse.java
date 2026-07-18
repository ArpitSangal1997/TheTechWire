package com.wireblog.dto;

import java.time.Instant;
import java.util.List;

public record CommentResponse(
        Long id,
        String body,
        AuthorResponse author,
        Long parentId,
        Instant createdAt,
        List<CommentResponse> replies
) {}
