package com.wireblog.controller;

import com.wireblog.dto.CommentRequest;
import com.wireblog.dto.CommentResponse;
import com.wireblog.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/post/{postId}")
    public List<CommentResponse> forPost(@PathVariable Long postId) {
        return commentService.threadForPost(postId);
    }

    @PostMapping("/post/{postId}")
    public CommentResponse add(@PathVariable Long postId, @Valid @RequestBody CommentRequest req) {
        return commentService.add(postId, req);
    }

    @DeleteMapping("/{commentId}")
    public void delete(@PathVariable Long commentId) {
        commentService.delete(commentId);
    }
}
