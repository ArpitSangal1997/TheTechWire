package com.wireblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record PostRequest(
        @NotBlank @Size(max = 220) String title,
        @Size(max = 500) String excerpt,
        @NotBlank String content,
        String coverImageUrl,
        Set<String> tags,
        boolean publish
) {}
