package com.wireblog.service;

import com.wireblog.dto.*;
import com.wireblog.exception.ApiException;
import com.wireblog.model.Post;
import com.wireblog.model.User;
import com.wireblog.repository.CommentRepository;
import com.wireblog.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CurrentUserResolver currentUserResolver;

    public PostService(PostRepository postRepository, CommentRepository commentRepository,
                        CurrentUserResolver currentUserResolver) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional
    public PostDetailResponse create(PostRequest req) {
        User author = currentUserResolver.requireCurrentUser();

        Post post = Post.builder()
                .title(req.title())
                .slug(uniqueSlug(req.title()))
                .excerpt(req.excerpt())
                .content(req.content())
                .coverImageUrl(req.coverImageUrl())
                .tags(req.tags())
                .author(author)
                .status(req.publish() ? Post.PostStatus.PUBLISHED : Post.PostStatus.DRAFT)
                .build();

        if (req.publish()) post.setPublishedAt(Instant.now());

        post = postRepository.save(post);
        return toDetail(post);
    }

    @Transactional
    public PostDetailResponse update(Long postId, PostRequest req) {
        User user = currentUserResolver.requireCurrentUser();
        Post post = postRepository.findById(postId).orElseThrow(() -> ApiException.notFound("Post not found."));

        if (!post.getAuthor().getId().equals(user.getId())) {
            throw ApiException.forbidden("You can only edit your own posts.");
        }

        post.setTitle(req.title());
        post.setExcerpt(req.excerpt());
        post.setContent(req.content());
        post.setCoverImageUrl(req.coverImageUrl());
        post.setTags(req.tags());

        if (req.publish() && post.getStatus() != Post.PostStatus.PUBLISHED) {
            post.setStatus(Post.PostStatus.PUBLISHED);
            post.setPublishedAt(Instant.now());
        }

        return toDetail(postRepository.save(post));
    }

    @Transactional
    public void delete(Long postId) {
        User user = currentUserResolver.requireCurrentUser();
        Post post = postRepository.findById(postId).orElseThrow(() -> ApiException.notFound("Post not found."));
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw ApiException.forbidden("You can only delete your own posts.");
        }
        postRepository.delete(post);
    }

    @Transactional
    public PostDetailResponse getBySlug(String slug) {
        Post post = postRepository.findBySlug(slug).orElseThrow(() -> ApiException.notFound("This post doesn't exist."));
        if (post.getStatus() != Post.PostStatus.PUBLISHED) {
            throw ApiException.notFound("This post doesn't exist.");
        }
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
        return toDetail(post);
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> listPublished(Pageable pageable) {
        return postRepository.findByStatus(Post.PostStatus.PUBLISHED, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> listByTag(String tag, Pageable pageable) {
        return postRepository.findByTagsContainingAndStatus(tag, Post.PostStatus.PUBLISHED, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> search(String query, Pageable pageable) {
        return postRepository.findByTitleContainingIgnoreCaseAndStatus(query, Post.PostStatus.PUBLISHED, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> myPosts(Pageable pageable) {
        User user = currentUserResolver.requireCurrentUser();
        return postRepository.findByAuthorId(user.getId(), pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getForEditing(Long postId) {
        User user = currentUserResolver.requireCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> ApiException.notFound("Post not found."));
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw ApiException.forbidden("You can only edit your own posts.");
        }
        return toDetail(post);
    }

    @Transactional
    public PostSummaryResponse publish(Long postId) {
        Post post = requireOwnedPost(postId);
        if (post.getStatus() != Post.PostStatus.PUBLISHED) {
            post.setStatus(Post.PostStatus.PUBLISHED);
            post.setPublishedAt(Instant.now());
        }
        return toSummary(postRepository.save(post));
    }

    @Transactional
    public PostSummaryResponse archive(Long postId) {
        Post post = requireOwnedPost(postId);
        post.setStatus(Post.PostStatus.ARCHIVED);
        return toSummary(postRepository.save(post));
    }

    void incrementShareCount(Post post) {
        post.setShareCount(post.getShareCount() + 1);
        postRepository.save(post);
    }

    private String uniqueSlug(String title) {
        String base = slugify(title);
        String candidate = base;
        int suffix = 1;
        while (postRepository.existsBySlug(candidate)) {
            candidate = base + "-" + (++suffix);
        }
        return candidate;
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String stripped = Pattern.compile("[^\\p{ASCII}]").matcher(normalized).replaceAll("");
        String slug = stripped.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").trim().replaceAll("\\s+", "-");
        return slug.isBlank() ? "post-" + System.currentTimeMillis() : slug;
    }

    private PostSummaryResponse toSummary(Post p) {
        long commentCount = commentRepository.countByPostId(p.getId());
        return new PostSummaryResponse(p.getId(), p.getTitle(), p.getSlug(), p.getExcerpt(), p.getCoverImageUrl(),
                copyTags(p), p.getStatus().name(), toAuthor(p.getAuthor()), p.getViewCount(), p.getShareCount(),
                commentCount, p.getPublishedAt());
    }

    private PostDetailResponse toDetail(Post p) {
        long commentCount = commentRepository.countByPostId(p.getId());
        return new PostDetailResponse(p.getId(), p.getTitle(), p.getSlug(), p.getExcerpt(), p.getContent(), p.getCoverImageUrl(),
                copyTags(p), p.getStatus().name(), toAuthor(p.getAuthor()), p.getViewCount(), p.getShareCount(), commentCount,
                p.getPublishedAt(), p.getUpdatedAt());
    }

    private Set<String> copyTags(Post p) {
        return p.getTags() == null ? Set.of() : new LinkedHashSet<>(p.getTags());
    }

    private AuthorResponse toAuthor(User u) {
        return new AuthorResponse(u.getId(), u.getDisplayName(), u.getHandle(), u.getAvatarUrl());
    }

    Post requirePost(Long id) {
        return postRepository.findById(id).orElseThrow(() -> ApiException.notFound("Post not found."));
    }

    private Post requireOwnedPost(Long id) {
        User user = currentUserResolver.requireCurrentUser();
        Post post = requirePost(id);
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw ApiException.forbidden("You can only manage your own posts.");
        }
        return post;
    }
}
