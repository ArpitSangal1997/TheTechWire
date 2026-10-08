package com.wireblog.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "community_groups") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CommunityGroup {
    public enum Visibility { PUBLIC, PRIVATE }
    public enum JoinPolicy { OPEN, REQUEST, INVITE_ONLY }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 180, unique = true) private String slug;
    @Column(length = 500) private String description;
    @ManyToOne(optional = false) @JoinColumn(name = "creator_id") private User creator;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) @Builder.Default
    private JoinPolicy joinPolicy = JoinPolicy.OPEN;
    @Column(length = 500) private String topics;
    @Column(length = 2000) private String rules;
    @Column(nullable = false) private Instant createdAt;
    @PrePersist void created() { if (createdAt == null) createdAt = Instant.now(); }
}
