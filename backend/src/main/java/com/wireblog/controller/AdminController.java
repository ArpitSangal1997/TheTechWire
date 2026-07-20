package com.wireblog.controller;

import com.wireblog.dto.SponsorshipRequest;
import com.wireblog.dto.SponsorshipResponse;
import com.wireblog.dto.AdminUserResponse;
import com.wireblog.dto.AuthorResponse;
import com.wireblog.dto.FlagRequest;
import com.wireblog.dto.ModerationCommentResponse;
import com.wireblog.exception.ApiException;
import com.wireblog.model.Comment;
import com.wireblog.model.User;
import com.wireblog.repository.CommentRepository;
import com.wireblog.repository.UserRepository;
import com.wireblog.service.SponsorshipService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Everything here is gated to ROLE_ADMIN in SecurityConfig — this is the
 * "you and me" back office: manage brand deals, moderate content, see
 * platform-wide numbers.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SponsorshipService sponsorshipService;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public AdminController(SponsorshipService sponsorshipService, CommentRepository commentRepository, UserRepository userRepository) {
        this.sponsorshipService = sponsorshipService;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/sponsorships")
    public List<SponsorshipResponse> listSponsorships() {
        return sponsorshipService.listAll();
    }

    @PostMapping("/sponsorships")
    public SponsorshipResponse createSponsorship(@Valid @RequestBody SponsorshipRequest req) {
        return sponsorshipService.create(req);
    }

    @PutMapping("/sponsorships/{id}")
    public SponsorshipResponse updateSponsorship(@PathVariable Long id, @Valid @RequestBody SponsorshipRequest req) {
        return sponsorshipService.update(id, req);
    }

    @DeleteMapping("/sponsorships/{id}")
    public void deleteSponsorship(@PathVariable Long id) {
        sponsorshipService.delete(id);
    }

    @GetMapping("/comments/flagged")
    public List<ModerationCommentResponse> flaggedComments() {
        return commentRepository.findByFlaggedTrueOrderByCreatedAtDesc().stream().map(this::toModerationComment).toList();
    }

    @PatchMapping("/comments/{id}/flag")
    public ModerationCommentResponse setFlag(@PathVariable Long id, @Valid @RequestBody FlagRequest request) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> ApiException.notFound("Comment not found."));
        comment.setFlagged(request.flagged());
        return toModerationComment(commentRepository.save(comment));
    }

    @DeleteMapping("/comments/{id}")
    public void deleteComment(@PathVariable Long id) {
        if (!commentRepository.existsById(id)) throw ApiException.notFound("Comment not found.");
        commentRepository.deleteById(id);
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return userRepository.findAll().stream().map(this::toAdminUser).toList();
    }

    @PatchMapping("/users/{id}/enabled")
    public AdminUserResponse setUserEnabled(@PathVariable Long id, @Valid @RequestBody FlagRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User not found."));
        user.setEnabled(request.flagged());
        return toAdminUser(userRepository.save(user));
    }

    private ModerationCommentResponse toModerationComment(Comment comment) {
        User author = comment.getAuthor();
        return new ModerationCommentResponse(comment.getId(), comment.getPost().getId(), comment.getPost().getTitle(),
                comment.getBody(), new AuthorResponse(author.getId(), author.getDisplayName(), author.getHandle(), author.getAvatarUrl()),
                comment.isFlagged(), comment.getCreatedAt());
    }

    private AdminUserResponse toAdminUser(User user) {
        return new AdminUserResponse(user.getId(), user.getDisplayName(), user.getHandle(), user.getEmail(),
                user.getRole().name(), user.isEnabled(), user.getCreatedAt());
    }
}
