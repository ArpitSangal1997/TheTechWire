package com.wireblog.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "group_posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private CommunityGroup group;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne
    @JoinColumn(name = "trail_id")
    private StoryTrail trail;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false, length = 5000)
    private String body;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void created() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
