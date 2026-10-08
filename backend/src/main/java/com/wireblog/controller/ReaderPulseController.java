package com.wireblog.controller;

import com.wireblog.dto.ReaderPulseResponse;
import com.wireblog.dto.ReaderPulseVoteRequest;
import com.wireblog.service.ReaderPulseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pulses/posts")
public class ReaderPulseController {

    private final ReaderPulseService readerPulseService;

    public ReaderPulseController(ReaderPulseService readerPulseService) {
        this.readerPulseService = readerPulseService;
    }

    @GetMapping("/{postId}")
    public ReaderPulseResponse getForPost(@PathVariable Long postId) {
        return readerPulseService.getForPost(postId);
    }

    @PostMapping("/{postId}")
    public ReaderPulseResponse vote(@PathVariable Long postId, @Valid @RequestBody ReaderPulseVoteRequest request) {
        return readerPulseService.vote(postId, request);
    }
}
