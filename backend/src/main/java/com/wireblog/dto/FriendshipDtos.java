package com.wireblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class FriendshipDtos {
    private FriendshipDtos() {}

    public record UserView(Long id, String displayName, String handle, String avatarUrl, String relationship) {}
    public record FriendView(Long id, String displayName, String handle, String avatarUrl, String relationship, Instant createdAt) {}
    public record Search(@NotBlank @Size(max = 80) String query) {}
}
