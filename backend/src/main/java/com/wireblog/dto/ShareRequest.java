package com.wireblog.dto;

import jakarta.validation.constraints.NotNull;

public record ShareRequest(
        @NotNull String channel,   // IN_APP, WHATSAPP, TWITTER, FACEBOOK, LINKEDIN, COPY_LINK, EMAIL
        String sharedToHandle      // required only when channel == IN_APP
) {}
