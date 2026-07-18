package com.wireblog.service;

import com.wireblog.dto.AuthorResponse;
import com.wireblog.dto.CommentRequest;
import com.wireblog.dto.CommentResponse;
import com.wireblog.exception.ApiException;
import com.wireblog.model.Comment;
import com.wireblog.model.Post;
import com.wireblog.model.User;
import com.wireblog.repository.CommentRepository;
import com.wireblog.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CurrentUserResolver currentUserResolver;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository,
                           CurrentUserResolver currentUserResolver, NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.currentUserResolver = currentUserResolver;
        this.notificationService = notificationService;
    }

    @Transactional
    public CommentResponse add(Long postId, CommentRequest req) {
        User author = currentUserResolver.requireCurrentUser();
        Post post = postRepository.findById(postId).orElseThrow(() -> ApiException.notFound("Post not found."));

        Comment parent = null;
        if (req.parentId() != null) {
            parent = commentRepository.findById(req.parentId())
                    .orElseThrow(() -> ApiException.notFound("The comment you're replying to no longer exists."));
        }

        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .parent(parent)
                .body(req.body())
                .build();

        comment = commentRepository.save(comment);

        if (parent != null) {
            notificationService.notifyReply(parent.getAuthor(), post, author);
        }

        return toResponse(comment, List.of());
    }

    @Transactional
    public void delete(Long commentId) {
        User user = currentUserResolver.requireCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Comment not found."));

        boolean isOwner = comment.getAuthor().getId().equals(user.getId());
        boolean isAdmin = "ADMIN".equals(user.getRole().name());
        if (!isOwner && !isAdmin) {
            throw ApiException.forbidden("You can only delete your own comments.");
        }
        commentRepository.delete(comment);
    }

    /** Builds a nested thread: top-level comments with their replies attached. */
    @Transactional(readOnly = true)
    public List<CommentResponse> threadForPost(Long postId) {
        List<Comment> all = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        Map<Long, List<Comment>> repliesByParent = new HashMap<>();
        List<Comment> topLevel = new ArrayList<>();

        for (Comment c : all) {
            if (c.getParent() == null) {
                topLevel.add(c);
            } else {
                repliesByParent.computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>()).add(c);
            }
        }

        List<CommentResponse> result = new ArrayList<>();
        for (Comment c : topLevel) {
            List<Comment> replies = repliesByParent.getOrDefault(c.getId(), List.of());
            List<CommentResponse> replyResponses = replies.stream().map(r -> toResponse(r, List.of())).toList();
            result.add(toResponse(c, replyResponses));
        }
        return result;
    }

    private CommentResponse toResponse(Comment c, List<CommentResponse> replies) {
        User a = c.getAuthor();
        AuthorResponse author = new AuthorResponse(a.getId(), a.getDisplayName(), a.getHandle(), a.getAvatarUrl());
        Long parentId = c.getParent() != null ? c.getParent().getId() : null;
        return new CommentResponse(c.getId(), c.getBody(), author, parentId, c.getCreatedAt(), replies);
    }
}
