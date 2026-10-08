package com.wireblog.service;

import com.wireblog.dto.PostSummaryResponse;
import com.wireblog.dto.StoryTrailDetailResponse;
import com.wireblog.dto.StoryTrailRefResponse;
import com.wireblog.dto.StoryTrailSummaryResponse;
import com.wireblog.dto.GroupPostTrailResponse;
import com.wireblog.exception.ApiException;
import com.wireblog.model.GroupPost;
import com.wireblog.model.CommunityGroup;
import com.wireblog.model.Post;
import com.wireblog.model.Role;
import com.wireblog.model.StoryTrail;
import com.wireblog.model.User;
import com.wireblog.repository.PostRepository;
import com.wireblog.repository.GroupPostRepository;
import com.wireblog.repository.GroupPostCommentRepository;
import com.wireblog.repository.StoryTrailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StoryTrailService {

    private final StoryTrailRepository storyTrailRepository;
    private final PostRepository postRepository;
    private final GroupPostRepository groupPostRepository;
    private final GroupPostCommentRepository groupPostCommentRepository;
    private final PostService postService;
    private final CurrentUserResolver currentUserResolver;

    public StoryTrailService(StoryTrailRepository storyTrailRepository, PostRepository postRepository,
                             GroupPostRepository groupPostRepository,
                             GroupPostCommentRepository groupPostCommentRepository,
                             PostService postService, CurrentUserResolver currentUserResolver) {
        this.storyTrailRepository = storyTrailRepository;
        this.postRepository = postRepository;
        this.groupPostRepository = groupPostRepository;
        this.groupPostCommentRepository = groupPostCommentRepository;
        this.postService = postService;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public Page<StoryTrailSummaryResponse> list(Pageable pageable) {
                User actor = currentUserResolver.currentUserIfAuthenticated().orElse(null);
                if (actor == null || actor.getRole() != Role.ADMIN) {
                        return storyTrailRepository.findTrailsWithPublicContent(Post.PostStatus.PUBLISHED,
                                        CommunityGroup.Visibility.PUBLIC, pageable)
                                        .map(this::toSummary);
                }
        return storyTrailRepository.findAll(pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public StoryTrailDetailResponse getBySlug(String slug) {
        StoryTrail trail = storyTrailRepository.findBySlug(slug)
                .orElseThrow(() -> ApiException.notFound("Story trail not found."));
        List<PostSummaryResponse> stories = postRepository
                .findByTrailIdAndStatus(trail.getId(), Post.PostStatus.PUBLISHED,
                        PageRequest.of(0, 50, Sort.by("publishedAt").descending()))
                .stream()
                .map(postService::toSummary)
                .toList();
        List<GroupPostTrailResponse> communityPosts = visibleCommunityPosts(trail.getId()).stream()
                .map(this::toCommunityPost).toList();
                if (stories.isEmpty() && communityPosts.isEmpty()) {
                        throw ApiException.notFound("Story trail not found.");
                }
        return new StoryTrailDetailResponse(trail.getId(), trail.getTitle(), trail.getSlug(),
                trail.getDescription(), stories.size() + communityPosts.size(), trail.getUpdatedAt(), stories, communityPosts);
    }

    StoryTrailSummaryResponse toSummary(StoryTrail trail) {
        long storyCount = postRepository.countByTrailIdAndStatus(trail.getId(), Post.PostStatus.PUBLISHED)
                + visibleCommunityPosts(trail.getId()).size();
        return new StoryTrailSummaryResponse(trail.getId(), trail.getTitle(), trail.getSlug(),
                trail.getDescription(), storyCount);
    }

    StoryTrailRefResponse toRef(StoryTrail trail) {
        if (trail == null) return null;
        return new StoryTrailRefResponse(trail.getId(), trail.getTitle(), trail.getSlug());
    }

    private GroupPostTrailResponse toCommunityPost(GroupPost post) {
        boolean canOpenGroup = post.getGroup().getVisibility() == CommunityGroup.Visibility.PUBLIC;
        return new GroupPostTrailResponse(post.getId(), post.getGroup().getId(), post.getGroup().getName(),
                post.getTitle(), post.getBody(), post.getAuthor().getId(), post.getAuthor().getDisplayName(),
                post.getAuthor().getHandle(), post.getCreatedAt(), groupPostCommentRepository.countByGroupPostId(post.getId()), canOpenGroup);
    }

    private List<GroupPost> visibleCommunityPosts(Long trailId) {
                return groupPostRepository.findByTrailIdOrderByCreatedAtDesc(trailId).stream()
                                .filter(post -> post.getGroup().getVisibility() == CommunityGroup.Visibility.PUBLIC)
                                .toList();
    }
}
