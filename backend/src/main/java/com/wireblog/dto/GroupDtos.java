package com.wireblog.dto;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import com.wireblog.model.CommunityGroup.JoinPolicy;
import com.wireblog.model.CommunityGroup.Visibility;
import com.wireblog.model.GroupJoinRequest.Status;
public final class GroupDtos {
 private GroupDtos() {}
 public record Create(@NotBlank @Size(max=120) String name, @Size(max=500) String description,
                      Visibility visibility, JoinPolicy joinPolicy, @Size(max=10) List<@NotBlank @Size(max=32) String> topics,
                      @Size(max=2000) String rules) {}
 public record Settings(@NotNull Visibility visibility, @NotNull JoinPolicy joinPolicy,
                        @Size(max=500) String description, @Size(max=10) List<@NotBlank @Size(max=32) String> topics,
                        @Size(max=2000) String rules) {}
 public record AddMember(@NotNull Long userId) {}
 public record JoinRequestDecision(@NotNull Status status) {}
 public record Member(Long id, String displayName, String handle, String role) {}
 public record View(Long id, String name, String slug, String description, Long creatorId, String creatorName,
                    String creatorHandle, Instant createdAt, int memberCount, Visibility visibility,
                    JoinPolicy joinPolicy, List<String> topics, String rules, boolean isMember,
                    boolean canManage, boolean joinRequestPending) {}
 public record JoinRequestView(Long id, Long userId, String displayName, String handle,
                               String status, Instant createdAt) {}
 public record PostRequest(@NotBlank @Size(max=140) String title, @NotBlank @Size(max=5000) String body,
						   @Size(max=160) String trailTitle) {}
 public record CommentRequest(@NotBlank @Size(max=5000) String body, Long parentId) {}
 public record CommentView(Long id, String body, Long authorId, String authorName, String authorHandle,
						   Instant createdAt, List<CommentView> replies) {}
 public record PostView(Long id, String title, String body, Long authorId, String authorName, String authorHandle,
						Instant createdAt, Long groupId, String groupName, StoryTrailRefResponse trail,
						long commentCount, List<CommentView> comments) {}
 public record Detail(Long id, String name, String slug, String description, Long creatorId, String creatorName,
                      String creatorHandle, Instant createdAt, int memberCount, Visibility visibility,
                      JoinPolicy joinPolicy, List<String> topics, String rules, List<Member> members,
                      List<PostView> posts, boolean isMember, boolean canManage, boolean joinRequestPending,
                      boolean canFollow, boolean canRequestJoin, List<JoinRequestView> joinRequests) {}
}
