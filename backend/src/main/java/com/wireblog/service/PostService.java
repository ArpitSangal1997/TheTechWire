package com.wireblog.service;

import com.wireblog.dto.*;
import com.wireblog.exception.ApiException;
import com.wireblog.model.Post;
import com.wireblog.model.StoryTrail;
import com.wireblog.model.User;
import com.wireblog.repository.CommentRepository;
import com.wireblog.repository.PostRepository;
import com.wireblog.repository.ReaderPulseRepository;
import com.wireblog.repository.ShareRepository;
import com.wireblog.repository.StoryTrailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.net.URI;
import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PostService {

    private static final Safelist POST_HTML = Safelist.relaxed()
            .removeTags("style")
            .removeAttributes("a", "target")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https");

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReaderPulseRepository readerPulseRepository;
    private final ShareRepository shareRepository;
    private final StoryTrailRepository storyTrailRepository;
    private final CurrentUserResolver currentUserResolver;

    public PostService(PostRepository postRepository, CommentRepository commentRepository,
                        ReaderPulseRepository readerPulseRepository,
                        ShareRepository shareRepository,
                        StoryTrailRepository storyTrailRepository,
                        CurrentUserResolver currentUserResolver) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.readerPulseRepository = readerPulseRepository;
        this.shareRepository = shareRepository;
        this.storyTrailRepository = storyTrailRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional
    public PostDetailResponse create(PostRequest req) {
        User author = currentUserResolver.requireCurrentUser();

        Post post = Post.builder()
                .title(req.title())
                .slug(uniqueSlug(req.title()))
                .excerpt(req.excerpt())
                .content(sanitizeContent(req.content()))
                .coverImageUrl(req.coverImageUrl())
                .tags(req.tags())
                .trail(resolveTrail(req.trailTitle()))
                .author(author)
                .status(req.publish() ? Post.PostStatus.PUBLISHED : Post.PostStatus.DRAFT)
                .build();

        if (req.publish()) post.setPublishedAt(Instant.now());

        post = postRepository.save(post);
        return toDetail(post);
    }

    @Transactional
    public PostDetailResponse shareLink(ShareLinkRequest request) {
        User author = currentUserResolver.requireCurrentUser();
        String sourceUrl = validateSourceUrl(request.url());
        String note = request.note() == null ? "" : request.note().trim();
        String title = request.title() == null || request.title().isBlank()
                ? URI.create(sourceUrl).getHost().replaceFirst("^www\\.", "")
                : request.title().trim();
        String safeNote = Jsoup.clean(note, Safelist.none());
        Post post = Post.builder()
                .title(title)
                .slug(uniqueSlug(title))
                .excerpt(safeNote.isBlank() ? null : safeNote)
                .content(safeNote)
                .sourceUrl(sourceUrl)
                .tags(Set.of())
                .author(author)
                .status(Post.PostStatus.PUBLISHED)
                .publishedAt(Instant.now())
                .build();
        return toDetail(postRepository.save(post));
    }

    @Transactional
    public PostDetailResponse updateSharedLink(Long postId, ShareLinkRequest request) {
        User author = currentUserResolver.requireCurrentUser();
        Post post = requirePost(postId);
        if (!post.getAuthor().getId().equals(author.getId())) {
            throw ApiException.forbidden("You can only edit your own posts.");
        }
        if (post.getSourceUrl() == null) {
            throw ApiException.badRequest("This story is not a shared link.");
        }
        String sourceUrl = validateSourceUrl(request.url());
        String note = request.note() == null ? "" : request.note().trim();
        String safeNote = Jsoup.clean(note, Safelist.none());
        String title = request.title() == null || request.title().isBlank()
                ? URI.create(sourceUrl).getHost().replaceFirst("^www\\.", "")
                : request.title().trim();
        if (!post.getTitle().equals(title)) {
            post.setTitle(title);
            post.setSlug(uniqueSlug(title));
        }
        post.setSourceUrl(sourceUrl);
        post.setExcerpt(safeNote.isBlank() ? null : safeNote);
        post.setContent(safeNote);
        return toDetail(postRepository.save(post));
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
        post.setContent(sanitizeContent(req.content()));
        post.setCoverImageUrl(req.coverImageUrl());
        post.setTags(req.tags());
        post.setTrail(resolveTrail(req.trailTitle()));

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
        deletePostGraph(post);
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
        return summarize(postRepository.findByStatus(Post.PostStatus.PUBLISHED, pageable));
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> listByTag(String tag, Pageable pageable) {
        return summarize(postRepository.findByTagsContainingAndStatus(tag, Post.PostStatus.PUBLISHED, pageable));
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> search(String query, Pageable pageable) {
        return summarize(postRepository.findByTitleContainingIgnoreCaseAndStatus(query, Post.PostStatus.PUBLISHED, pageable));
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

    @Transactional(readOnly = true)
    public Page<AdminPostResponse> adminPosts(Pageable pageable, String query) {
        Page<Post> posts = query == null || query.isBlank()
                ? postRepository.findAll(pageable)
                : postRepository.searchForAdmin(query.trim(), pageable);
        return posts.map(this::toAdminPost);
    }

    @Transactional
    public AdminPostResponse adminPublish(Long postId) {
        Post post = requirePost(postId);
        if (post.getStatus() != Post.PostStatus.PUBLISHED) {
            post.setStatus(Post.PostStatus.PUBLISHED);
            post.setPublishedAt(Instant.now());
        }
        return toAdminPost(postRepository.save(post));
    }

    @Transactional
    public AdminPostResponse adminArchive(Long postId) {
        Post post = requirePost(postId);
        post.setStatus(Post.PostStatus.ARCHIVED);
        return toAdminPost(postRepository.save(post));
    }

    @Transactional
    public void adminDelete(Long postId) {
        deletePostGraph(requirePost(postId));
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

    private String sanitizeContent(String content) {
        return Jsoup.clean(content, POST_HTML);
    }

    private String validateSourceUrl(String value) {
        if (value == null || value.isBlank() || value.length() > 2048) {
            throw ApiException.badRequest("Enter a valid story URL (up to 2048 characters).");
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null || scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw ApiException.badRequest("Story URLs must start with http:// or https://.");
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Enter a valid http:// or https:// story URL.");
        }
    }

    PostSummaryResponse toSummary(Post p) {
        long commentCount = commentRepository.countByPostId(p.getId());
        return new PostSummaryResponse(p.getId(), p.getTitle(), p.getSlug(), p.getExcerpt(), p.getCoverImageUrl(),
            p.getSourceUrl(), copyTags(p), p.getStatus().name(), toAuthor(p.getAuthor()), p.getViewCount(), p.getShareCount(),
                commentCount, toTrailRef(p.getTrail()), p.getPublishedAt());
    }

    private Page<PostSummaryResponse> summarize(Page<Post> posts) {
        List<Long> ids = posts.getContent().stream().map(Post::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        if (!ids.isEmpty()) commentRepository.countGroupedByPostIds(ids)
                .forEach(row -> counts.put((Long) row[0], (Long) row[1]));
        return posts.map(p -> toSummary(p, counts.getOrDefault(p.getId(), 0L)));
    }

    private PostSummaryResponse toSummary(Post p, long commentCount) {
        return new PostSummaryResponse(p.getId(), p.getTitle(), p.getSlug(), p.getExcerpt(), p.getCoverImageUrl(),
            p.getSourceUrl(), copyTags(p), p.getStatus().name(), toAuthor(p.getAuthor()), p.getViewCount(), p.getShareCount(),
                commentCount, toTrailRef(p.getTrail()), p.getPublishedAt());
    }

    private PostDetailResponse toDetail(Post p) {
        long commentCount = commentRepository.countByPostId(p.getId());
        return new PostDetailResponse(p.getId(), p.getTitle(), p.getSlug(), p.getExcerpt(), p.getContent(), p.getCoverImageUrl(),
            p.getSourceUrl(), copyTags(p), p.getStatus().name(), toAuthor(p.getAuthor()), p.getViewCount(), p.getShareCount(), commentCount,
                toTrailRef(p.getTrail()), p.getPublishedAt(), p.getUpdatedAt());
    }

    private Set<String> copyTags(Post p) {
        return p.getTags() == null ? Set.of() : new LinkedHashSet<>(p.getTags());
    }

    private AuthorResponse toAuthor(User u) {
        return new AuthorResponse(u.getId(), u.getDisplayName(), u.getHandle(), u.getAvatarUrl());
    }

    private AdminPostResponse toAdminPost(Post p) {
        long commentCount = commentRepository.countByPostId(p.getId());
        return new AdminPostResponse(p.getId(), p.getTitle(), p.getSlug(), p.getStatus().name(), toAuthor(p.getAuthor()),
                toTrailRef(p.getTrail()), p.getViewCount(), p.getShareCount(), commentCount, p.getCreatedAt(),
                p.getPublishedAt(), p.getUpdatedAt());
    }

    private StoryTrailRefResponse toTrailRef(StoryTrail trail) {
        if (trail == null) return null;
        return new StoryTrailRefResponse(trail.getId(), trail.getTitle(), trail.getSlug());
    }

    private StoryTrail resolveTrail(String title) {
        if (title == null || title.isBlank()) return null;
        String cleaned = title.trim();
        return storyTrailRepository.findByTitleIgnoreCase(cleaned)
                .orElseGet(() -> storyTrailRepository.save(StoryTrail.builder()
                        .title(cleaned)
                        .slug(uniqueTrailSlug(cleaned))
                        .description("Follow the latest reporting and context on " + cleaned + ".")
                        .build()));
    }

    private String uniqueTrailSlug(String title) {
        String base = slugify(title);
        String candidate = base;
        int suffix = 1;
        while (storyTrailRepository.existsBySlug(candidate)) {
            candidate = base + "-" + (++suffix);
        }
        return candidate;
    }

    Post requirePost(Long id) {
        return postRepository.findById(id).orElseThrow(() -> ApiException.notFound("Post not found."));
    }

    public void deletePostGraph(Post post) {
        Long postId = post.getId();
        shareRepository.deleteByPostId(postId);
        readerPulseRepository.deleteByPostId(postId);
        commentRepository.deleteByPostId(postId);
        postRepository.delete(post);
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
