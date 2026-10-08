package com.wireblog.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "group_members", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"})) @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupMember {
    public enum MembershipRole { MEMBER, GROUP_ADMIN }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "group_id") private CommunityGroup group;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MembershipRole role;
    @Column(nullable = false) private Instant createdAt;
    @PrePersist void created() { if (createdAt == null) createdAt = Instant.now(); }
}
