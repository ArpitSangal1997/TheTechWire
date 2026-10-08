package com.wireblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShareLinkRequest(
        @NotBlank @Size(max = 2048) String url,
        @Size(max = 220) String title,
        @Size(max = 500) String note
) {}
