package com.wireblog.dto;

public record ReaderPulseOptionResponse(
        String choice,
        String label,
        long count,
        int percentage
) {}
