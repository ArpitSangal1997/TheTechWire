package com.wireblog.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(nullable = false, unique = true, length = 240)
    private String slug; // url-friendly, used for /post/{slug} and share links

    @Column(length = 500)
    private String excerpt;

    @Column(length = 2048)
    private String sourceUrl;

    @Lob
    @Column(nullable = false)
    private String content; // stored as HTML/markdown from the rich editor

    private String coverImageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trail_id")
    private StoryTrail trail;

    @ElementCollection
    @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PostStatus status = PostStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private long viewCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private long shareCount = 0L;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public enum PostStatus {
        DRAFT, PUBLISHED, ARCHIVED
    }
}
