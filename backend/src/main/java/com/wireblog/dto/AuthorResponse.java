package com.wireblog.dto;

public record AuthorResponse(
        Long id,
        String displayName,
        String handle,
        String avatarUrl
) {}
