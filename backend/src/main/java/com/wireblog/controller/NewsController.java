package com.wireblog.controller;

import com.wireblog.dto.NewsHeadlineResponse;
import com.wireblog.service.NewsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/headlines")
    public List<NewsHeadlineResponse> headlines(@RequestParam(required = false) String category) {
        return (category == null || category.isBlank()) ? newsService.latest() : newsService.byCategory(category);
    }
}
