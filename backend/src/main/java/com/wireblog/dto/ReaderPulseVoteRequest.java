package com.wireblog.dto;

import jakarta.validation.constraints.NotBlank;

public record ReaderPulseVoteRequest(@NotBlank String choice) {}
