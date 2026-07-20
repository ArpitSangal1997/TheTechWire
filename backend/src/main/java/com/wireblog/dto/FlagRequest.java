package com.wireblog.dto;

import jakarta.validation.constraints.NotNull;

public record FlagRequest(@NotNull Boolean flagged) {}
