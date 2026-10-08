package com.wireblog.controller;

import com.wireblog.dto.SponsorshipRequest;
import com.wireblog.dto.SponsorshipResponse;
import com.wireblog.dto.AdminUserResponse;
import com.wireblog.dto.AdminPostResponse;
import com.wireblog.dto.AuthorResponse;
import com.wireblog.dto.FlagRequest;
import com.wireblog.dto.ModerationCommentResponse;
import com.wireblog.dto.RoleUpdateRequest;
import com.wireblog.exception.ApiException;
import com.wireblog.model.Comment;
import com.wireblog.model.Post;
import com.wireblog.model.Role;
import com.wireblog.model.User;
import com.wireblog.model.CommunityGroup;
import com.wireblog.repository.CommunityGroupRepository;
import com.wireblog.repository.CommentRepository;
import com.wireblog.repository.FriendshipRepository;
import com.wireblog.repository.GroupMemberRepository;
import com.wireblog.repository.GroupJoinRequestRepository;
import com.wireblog.repository.GroupPostCommentRepository;
import com.wireblog.repository.GroupPostRepository;
import com.wireblog.repository.NotificationRepository;
import com.wireblog.repository.PostRepository;
import com.wireblog.repository.ShareRepository;
import com.wireblog.repository.UserRepository;
import com.wireblog.service.CurrentUserResolver;
import com.wireblog.service.PostService;
import com.wireblog.service.SponsorshipService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
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
    private final PostService postService;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ShareRepository shareRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CommunityGroupRepository communityGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final GroupPostRepository groupPostRepository;
    private final GroupPostCommentRepository groupPostCommentRepository;
    private final FriendshipRepository friendshipRepository;
    private final CurrentUserResolver currentUserResolver;

    public AdminController(SponsorshipService sponsorshipService, PostService postService,
                           CommentRepository commentRepository, PostRepository postRepository,
                           ShareRepository shareRepository, NotificationRepository notificationRepository,
                           UserRepository userRepository, CommunityGroupRepository communityGroupRepository,
                           GroupMemberRepository groupMemberRepository,
                           GroupJoinRequestRepository groupJoinRequestRepository,
                           GroupPostRepository groupPostRepository,
                           GroupPostCommentRepository groupPostCommentRepository,
                           FriendshipRepository friendshipRepository, CurrentUserResolver currentUserResolver) {
        this.sponsorshipService = sponsorshipService;
        this.postService = postService;
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.shareRepository = shareRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.communityGroupRepository = communityGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupJoinRequestRepository = groupJoinRequestRepository;
        this.groupPostRepository = groupPostRepository;
        this.groupPostCommentRepository = groupPostCommentRepository;
        this.friendshipRepository = friendshipRepository;
        this.currentUserResolver = currentUserResolver;
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
    @Transactional(readOnly = true)
    public List<ModerationCommentResponse> flaggedComments() {
        return commentRepository.findByFlaggedTrueOrderByCreatedAtDesc().stream().map(this::toModerationComment).toList();
    }

    @PatchMapping("/comments/{id}/flag")
    @Transactional
    public ModerationCommentResponse setFlag(@PathVariable Long id, @Valid @RequestBody FlagRequest request) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> ApiException.notFound("Comment not found."));
        comment.setFlagged(request.flagged());
        return toModerationComment(commentRepository.save(comment));
    }

    @DeleteMapping("/comments/{id}")
    public void deleteComment(@PathVariable Long id) {
        if (!commentRepository.existsById(id)) throw ApiException.notFound("Comment not found.");
        commentRepository.deleteByParentId(id);
        commentRepository.deleteById(id);
    }

    @GetMapping("/posts")
    public Page<AdminPostResponse> posts(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(required = false) String q) {
        return postService.adminPosts(PageRequest.of(page, size, Sort.by("createdAt").descending()), q);
    }

    @PostMapping("/posts/{id}/publish")
    public AdminPostResponse publishPost(@PathVariable Long id) {
        return postService.adminPublish(id);
    }

    @PostMapping("/posts/{id}/archive")
    public AdminPostResponse archivePost(@PathVariable Long id) {
        return postService.adminArchive(id);
    }

    @DeleteMapping("/posts/{id}")
    public void deletePost(@PathVariable Long id) {
        postService.adminDelete(id);
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users(@RequestParam(defaultValue = "") String q) {
        List<User> results = q.isBlank() ? userRepository.findAll() : userRepository.searchUsersForAdmin(q.trim());
        return results.stream().map(this::toAdminUser).toList();
    }

    @PatchMapping("/users/{id}/enabled")
    public AdminUserResponse setUserEnabled(@PathVariable Long id, @Valid @RequestBody FlagRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User not found."));
        if (user.getId().equals(currentUserResolver.requireCurrentUser().getId())) {
            throw ApiException.badRequest("You cannot suspend your own account.");
        }
        user.setEnabled(request.flagged());
        return toAdminUser(userRepository.save(user));
    }

    @PatchMapping("/users/{id}/role")
    public AdminUserResponse setUserRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User not found."));
        if (user.getId().equals(currentUserResolver.requireCurrentUser().getId())) {
            throw ApiException.badRequest("You cannot change your own role.");
        }
        user.setRole(parseRole(request.role()));
        return toAdminUser(userRepository.save(user));
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public void deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User not found."));
        if (user.getId().equals(currentUserResolver.requireCurrentUser().getId())) {
            throw ApiException.badRequest("You cannot delete your own account.");
        }
        List<Post> posts = postRepository.findAllByAuthorId(user.getId());
        posts.forEach(postService::deletePostGraph);
        commentRepository.deleteRepliesToAuthor(user.getId());
        commentRepository.deleteByAuthorId(user.getId());
        shareRepository.deleteBySharedToId(user.getId());
        shareRepository.deleteBySharedById(user.getId());
        notificationRepository.deleteByUserId(user.getId());
        friendshipRepository.deleteByRequesterIdOrRecipientId(user.getId(), user.getId());
        groupJoinRequestRepository.deleteByUserId(user.getId());
        groupPostCommentRepository.deleteByAuthorId(user.getId());
        groupMemberRepository.deleteByUserId(user.getId());
        groupPostRepository.deleteByAuthorId(user.getId());
        List<CommunityGroup> ownedGroups = communityGroupRepository.findByCreatorId(user.getId());
        ownedGroups.forEach(group -> {
            groupPostRepository.findByGroupIdOrderByCreatedAtDesc(group.getId())
                    .forEach(post -> groupPostCommentRepository.deleteByGroupPostId(post.getId()));
            groupPostRepository.deleteByGroupId(group.getId());
            groupMemberRepository.deleteByGroupId(group.getId());
            groupJoinRequestRepository.deleteByGroupId(group.getId());
            communityGroupRepository.delete(group);
        });
        userRepository.delete(user);
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

    private Role parseRole(String role) {
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Unknown role.");
        }
    }
}
