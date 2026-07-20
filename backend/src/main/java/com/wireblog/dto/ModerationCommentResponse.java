package com.wireblog.dto;

import java.time.Instant;

public record ModerationCommentResponse(Long id, Long postId, String postTitle, String body,
                                        AuthorResponse author, boolean flagged, Instant createdAt) {}
