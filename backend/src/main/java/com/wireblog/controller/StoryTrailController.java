package com.wireblog.controller;

import com.wireblog.dto.StoryTrailDetailResponse;
import com.wireblog.dto.StoryTrailSummaryResponse;
import com.wireblog.service.StoryTrailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trails")
public class StoryTrailController {

    private final StoryTrailService storyTrailService;

    public StoryTrailController(StoryTrailService storyTrailService) {
        this.storyTrailService = storyTrailService;
    }

    @GetMapping
    public Page<StoryTrailSummaryResponse> list(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "12") int size) {
        return storyTrailService.list(PageRequest.of(page, size, Sort.by("updatedAt").descending()));
    }

    @GetMapping("/{slug}")
    public StoryTrailDetailResponse getBySlug(@PathVariable String slug) {
        return storyTrailService.getBySlug(slug);
    }
}
