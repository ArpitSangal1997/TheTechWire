package com.wireblog.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String message,
        String link,
        boolean read,
        Instant createdAt
) {}
