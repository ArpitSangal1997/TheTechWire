package com.wireblog.controller;

import com.wireblog.dto.PostDetailResponse;
import com.wireblog.dto.PostRequest;
import com.wireblog.dto.PostSummaryResponse;
import com.wireblog.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public Page<PostSummaryResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "12") int size,
                                           @RequestParam(required = false) String tag,
                                           @RequestParam(required = false) String q) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (tag != null && !tag.isBlank()) return postService.listByTag(tag, pageable);
        if (q != null && !q.isBlank()) return postService.search(q, pageable);
        return postService.listPublished(pageable);
    }

    @GetMapping("/mine")
    public Page<PostSummaryResponse> mine(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return postService.myPosts(PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @GetMapping("/{id}/edit")
    public PostDetailResponse getForEditing(@PathVariable Long id) {
        return postService.getForEditing(id);
    }

    @GetMapping("/{slug}")
    public PostDetailResponse getOne(@PathVariable String slug) {
        return postService.getBySlug(slug);
    }

    @PostMapping
    public PostDetailResponse create(@Valid @RequestBody PostRequest req) {
        return postService.create(req);
    }

    @PutMapping("/{id}")
    public PostDetailResponse update(@PathVariable Long id, @Valid @RequestBody PostRequest req) {
        return postService.update(id, req);
    }

    @PostMapping("/{id}/publish")
    public PostSummaryResponse publish(@PathVariable Long id) {
        return postService.publish(id);
    }

    @PostMapping("/{id}/archive")
    public PostSummaryResponse archive(@PathVariable Long id) {
        return postService.archive(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        postService.delete(id);
    }
}
