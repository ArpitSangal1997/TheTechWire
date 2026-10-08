package com.wireblog.dto;

import java.util.List;

public record ReaderPulseResponse(
        String question,
        long totalVotes,
        List<ReaderPulseOptionResponse> options
) {}
