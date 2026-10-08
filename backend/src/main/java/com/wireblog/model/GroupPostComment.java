package com.wireblog.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "group_post_comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPostComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "group_post_id", nullable = false)
    private GroupPost groupPost;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private GroupPostComment parent;

    @Column(nullable = false, length = 5000)
    private String body;

    @Column(nullable = false)
    @Builder.Default
    private boolean flagged = false;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void created() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
