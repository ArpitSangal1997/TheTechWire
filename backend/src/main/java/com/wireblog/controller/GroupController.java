package com.wireblog.controller;

import com.wireblog.dto.GroupDtos.*;
import com.wireblog.exception.ApiException;
import com.wireblog.model.*;
import com.wireblog.repository.*;
import com.wireblog.service.CurrentUserResolver;
import jakarta.validation.Valid;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import com.wireblog.dto.StoryTrailRefResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
	private final CommunityGroupRepository groups;
	private final GroupMemberRepository members;
	private final GroupPostRepository posts;
	private final GroupPostCommentRepository postComments;
	private final StoryTrailRepository trails;
	private final UserRepository users;
	private final CurrentUserResolver current;
	private final GroupJoinRequestRepository joinRequests;

	public GroupController(CommunityGroupRepository groups, GroupMemberRepository members, GroupPostRepository posts,
						   GroupPostCommentRepository postComments, StoryTrailRepository trails,
						   UserRepository users, CurrentUserResolver current, GroupJoinRequestRepository joinRequests) {
		this.groups = groups;
		this.members = members;
		this.posts = posts;
		this.postComments = postComments;
		this.trails = trails;
		this.users = users;
		this.current = current;
		this.joinRequests = joinRequests;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public List<View> list() {
		User actor = current.currentUserIfAuthenticated().orElse(null);
		return groups.findAllByOrderByCreatedAtDesc().stream()
				.filter(group -> group.getVisibility() == CommunityGroup.Visibility.PUBLIC || actor != null)
				.map(group -> view(group, actor)).toList();
	}

	@GetMapping("/{id}")
	@Transactional(readOnly = true)
	public Detail detail(@PathVariable Long id) {
		CommunityGroup group = require(id);
		User actor = current.currentUserIfAuthenticated().orElse(null);
		return detail(group, actor);
	}

	@PostMapping
	@Transactional
	public View create(@Valid @RequestBody Create request) {
		User owner = current.requireCurrentUser();
		CommunityGroup.Visibility visibility = request.visibility() == null
				? CommunityGroup.Visibility.PUBLIC : request.visibility();
		CommunityGroup.JoinPolicy joinPolicy = request.joinPolicy() == null
				? defaultJoinPolicy(visibility) : request.joinPolicy();
		validateJoinPolicy(visibility, joinPolicy);
		CommunityGroup group = groups.save(CommunityGroup.builder()
				.name(request.name().trim())
				.slug(slug(request.name()))
				.description(clean(request.description()))
				.visibility(visibility)
				.joinPolicy(joinPolicy)
				.topics(cleanTopics(request.topics()))
				.rules(clean(request.rules()))
				.creator(owner)
				.build());
		members.save(GroupMember.builder().group(group).user(owner)
				.role(GroupMember.MembershipRole.GROUP_ADMIN).build());
		return view(group, owner);
	}

	@PatchMapping("/{id}")
	@Transactional
	public Detail update(@PathVariable Long id, @Valid @RequestBody Settings request) {
		CommunityGroup group = require(id);
		User actor = current.requireCurrentUser();
		requireManager(group, actor);
		validateJoinPolicy(request.visibility(), request.joinPolicy());
		group.setVisibility(request.visibility());
		group.setJoinPolicy(request.joinPolicy());
		group.setDescription(clean(request.description()));
		group.setTopics(cleanTopics(request.topics()));
		group.setRules(clean(request.rules()));
		return detail(group, actor);
	}

	@PostMapping("/{id}/follow")
	@Transactional
	public Detail follow(@PathVariable Long id) {
		CommunityGroup group = require(id);
		User actor = current.requireCurrentUser();
		if (group.getVisibility() != CommunityGroup.Visibility.PUBLIC) {
			throw ApiException.forbidden("Private groups require an invitation or an approved join request.");
		}
		addMembership(group, actor);
		return detail(group, actor);
	}

	@PostMapping("/{id}/join-requests")
	@Transactional
	public Detail requestToJoin(@PathVariable Long id) {
		CommunityGroup group = require(id);
		User actor = current.requireCurrentUser();
		if (group.getVisibility() != CommunityGroup.Visibility.PRIVATE
				|| group.getJoinPolicy() != CommunityGroup.JoinPolicy.REQUEST) {
			throw ApiException.badRequest("This group is not accepting join requests.");
		}
		if (members.findByGroupIdAndUserId(id, actor.getId()).isPresent()) {
			throw ApiException.conflict("You are already a member of this group.");
		}
		GroupJoinRequest request = joinRequests.findByGroupIdAndUserId(id, actor.getId())
				.orElseGet(() -> GroupJoinRequest.builder().group(group).user(actor).build());
		if (request.getStatus() != GroupJoinRequest.Status.PENDING) {
			request.setStatus(GroupJoinRequest.Status.PENDING);
			request.setCreatedAt(java.time.Instant.now());
		}
		joinRequests.save(request);
		return detail(group, actor);
	}

	@PatchMapping("/{id}/join-requests/{requestId}")
	@Transactional
	public Detail decideJoinRequest(@PathVariable Long id, @PathVariable Long requestId,
									@Valid @RequestBody JoinRequestDecision decision) {
		CommunityGroup group = require(id);
		User actor = current.requireCurrentUser();
		requireManager(group, actor);
		if (decision.status() == GroupJoinRequest.Status.PENDING) {
			throw ApiException.badRequest("A join request must be approved or declined.");
		}
		GroupJoinRequest request = joinRequests.findByIdAndGroupId(requestId, id)
				.orElseThrow(() -> ApiException.notFound("Join request not found."));
		if (request.getStatus() != GroupJoinRequest.Status.PENDING) {
			throw ApiException.conflict("This join request has already been handled.");
		}
		request.setStatus(decision.status());
		if (decision.status() == GroupJoinRequest.Status.APPROVED) addMembership(group, request.getUser());
		return detail(group, actor);
	}

	@PostMapping("/{id}/members")
	@Transactional
	public Detail add(@PathVariable Long id, @Valid @RequestBody AddMember request) {
		CommunityGroup group = require(id);
		User actor = current.requireCurrentUser();
		requireManager(group, actor);
		User user = users.findById(request.userId()).filter(User::isEnabled)
				.orElseThrow(() -> ApiException.notFound("Active user not found."));
		addMembership(group, user);
		return detail(group, actor);
	}

	@DeleteMapping("/{id}/members/{userId}")
	@Transactional
	public void remove(@PathVariable Long id, @PathVariable Long userId) {
		CommunityGroup group = require(id);
		User actor = current.requireCurrentUser();
		if (group.getCreator().getId().equals(userId)) {
			throw ApiException.badRequest("The group creator cannot be removed.");
		}
		if (!actor.getId().equals(userId)) requireManager(group, actor);
		members.findByGroupIdAndUserId(id, userId).ifPresent(members::delete);
	}

	@PostMapping("/{id}/posts")
	@Transactional
	public PostView createPost(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
		CommunityGroup group = require(id);
		User author = current.requireCurrentUser();
		if (members.findByGroupIdAndUserId(id, author.getId()).isEmpty()) {
			throw ApiException.forbidden("Join the group before posting.");
		}
		GroupPost post = posts.save(GroupPost.builder().group(group).author(author)
				.title(request.title().trim()).body(Jsoup.clean(request.body().trim(), Safelist.none()))
				.trail(resolveTrail(request.trailTitle())).build());
		return postView(post);
	}

	@PostMapping("/{id}/posts/{postId}/comments")
	@Transactional
	public PostView addComment(@PathVariable Long id, @PathVariable Long postId,
							   @Valid @RequestBody CommentRequest request) {
		CommunityGroup group = require(id);
		User author = current.requireCurrentUser();
		if (members.findByGroupIdAndUserId(id, author.getId()).isEmpty()) {
			throw ApiException.forbidden("Join the group before commenting.");
		}
		GroupPost post = posts.findByIdAndGroupId(postId, group.getId())
				.orElseThrow(() -> ApiException.notFound("Group post not found."));
		GroupPostComment parent = null;
		if (request.parentId() != null) {
			parent = postComments.findByIdAndGroupPostId(request.parentId(), postId)
					.orElseThrow(() -> ApiException.notFound("Comment not found."));
			if (parent.getParent() != null) throw ApiException.badRequest("Replies can only be one level deep.");
		}
		postComments.save(GroupPostComment.builder().groupPost(post).author(author).parent(parent)
				.body(Jsoup.clean(request.body().trim(), Safelist.none())).build());
		return postView(post);
	}

	@DeleteMapping("/{id}/posts/{postId}/comments/{commentId}")
	@Transactional
	public void deleteComment(@PathVariable Long id, @PathVariable Long postId, @PathVariable Long commentId) {
		CommunityGroup group = require(id);
		GroupPost post = posts.findByIdAndGroupId(postId, group.getId())
				.orElseThrow(() -> ApiException.notFound("Group post not found."));
		GroupPostComment comment = postComments.findByIdAndGroupPostId(commentId, post.getId())
				.orElseThrow(() -> ApiException.notFound("Comment not found."));
		User actor = current.requireCurrentUser();
		if (!comment.getAuthor().getId().equals(actor.getId())) requireManager(group, actor);
		postComments.deleteByParentId(commentId);
		postComments.delete(comment);
	}

	@DeleteMapping("/{id}/posts/{postId}")
	@Transactional
	public void deletePost(@PathVariable Long id, @PathVariable Long postId) {
		CommunityGroup group = require(id);
		requireManager(group, current.requireCurrentUser());
		GroupPost post = posts.findByIdAndGroupId(postId, group.getId())
				.orElseThrow(() -> ApiException.notFound("Group post not found."));
		postComments.deleteByGroupPostId(post.getId());
		posts.delete(post);
	}

	@DeleteMapping("/{id}")
	@Transactional
	public void delete(@PathVariable Long id) {
		CommunityGroup group = require(id);
		User actor = current.requireCurrentUser();
		if (!group.getCreator().getId().equals(actor.getId()) && actor.getRole() != Role.ADMIN) {
			throw ApiException.forbidden("Only the group creator or a platform admin can delete this group.");
		}
		posts.findByGroupIdOrderByCreatedAtDesc(id).forEach(post -> postComments.deleteByGroupPostId(post.getId()));
		posts.deleteByGroupId(id);
		joinRequests.deleteByGroupId(id);
		members.deleteByGroupId(id);
		groups.delete(group);
	}

	private CommunityGroup require(Long id) {
		return groups.findById(id).orElseThrow(() -> ApiException.notFound("Group not found."));
	}

	private void requireManager(CommunityGroup group, User actor) {
		if (!canManage(group, actor)) throw ApiException.forbidden("Only a group admin can manage this group.");
	}

	private boolean canManage(CommunityGroup group, User actor) {
		return actor != null && (group.getCreator().getId().equals(actor.getId()) || actor.getRole() == Role.ADMIN
				|| members.findByGroupIdAndUserId(group.getId(), actor.getId())
				.map(member -> member.getRole() == GroupMember.MembershipRole.GROUP_ADMIN).orElse(false));
	}

	private Detail detail(CommunityGroup group, User actor) {
		boolean isMember = actor != null
				&& members.findByGroupIdAndUserId(group.getId(), actor.getId()).isPresent();
		boolean canManage = canManage(group, actor);
		boolean canViewContent = group.getVisibility() == CommunityGroup.Visibility.PUBLIC || isMember || canManage;
		List<GroupMember> memberRows = canViewContent ? members.findByGroupId(group.getId()) : List.of();
		List<Member> groupMembers = memberRows.stream()
				.map(member -> new Member(member.getUser().getId(), member.getUser().getDisplayName(),
						member.getUser().getHandle(), member.getRole().name())).toList();
		List<PostView> groupPosts = canViewContent
				? posts.findByGroupIdOrderByCreatedAtDesc(group.getId()).stream().map(this::postView).toList()
				: List.of();
		boolean joinRequestPending = actor != null && joinRequests.findByGroupIdAndUserId(group.getId(), actor.getId())
				.map(request -> request.getStatus() == GroupJoinRequest.Status.PENDING).orElse(false);
		boolean canRequestJoin = actor != null && !isMember && group.getVisibility() == CommunityGroup.Visibility.PRIVATE
				&& group.getJoinPolicy() == CommunityGroup.JoinPolicy.REQUEST && !joinRequestPending;
		boolean canFollow = actor != null && !isMember && group.getVisibility() == CommunityGroup.Visibility.PUBLIC;
		List<JoinRequestView> pendingRequests = canManage
				? joinRequests.findByGroupIdAndStatusOrderByCreatedAtAsc(group.getId(), GroupJoinRequest.Status.PENDING)
					.stream().map(request -> new JoinRequestView(request.getId(), request.getUser().getId(),
							request.getUser().getDisplayName(), request.getUser().getHandle(),
							request.getStatus().name(), request.getCreatedAt())).toList()
				: List.of();
		return new Detail(group.getId(), group.getName(), group.getSlug(), group.getDescription(),
				group.getCreator().getId(), group.getCreator().getDisplayName(), group.getCreator().getHandle(),
				group.getCreatedAt(), (int) members.countByGroupId(group.getId()), group.getVisibility(), group.getJoinPolicy(),
				splitTopics(group.getTopics()), group.getRules(), groupMembers, groupPosts, isMember, canManage,
				joinRequestPending, canFollow, canRequestJoin, pendingRequests);
	}

	private View view(CommunityGroup group, User actor) {
		boolean isMember = actor != null
				&& members.findByGroupIdAndUserId(group.getId(), actor.getId()).isPresent();
		boolean canManage = canManage(group, actor);
		boolean joinRequestPending = actor != null && joinRequests.findByGroupIdAndUserId(group.getId(), actor.getId())
				.map(request -> request.getStatus() == GroupJoinRequest.Status.PENDING).orElse(false);
		return new View(group.getId(), group.getName(), group.getSlug(), group.getDescription(),
				group.getCreator().getId(), group.getCreator().getDisplayName(), group.getCreator().getHandle(),
				group.getCreatedAt(), (int) members.countByGroupId(group.getId()), group.getVisibility(),
				group.getJoinPolicy(), splitTopics(group.getTopics()), group.getRules(), isMember, canManage,
				joinRequestPending);
	}

	private void addMembership(CommunityGroup group, User user) {
		members.findByGroupIdAndUserId(group.getId(), user.getId()).orElseGet(() -> members.save(
				GroupMember.builder().group(group).user(user).role(GroupMember.MembershipRole.MEMBER).build()));
		joinRequests.findByGroupIdAndUserId(group.getId(), user.getId())
				.filter(request -> request.getStatus() == GroupJoinRequest.Status.PENDING)
				.ifPresent(request -> request.setStatus(GroupJoinRequest.Status.APPROVED));
	}

	private CommunityGroup.JoinPolicy defaultJoinPolicy(CommunityGroup.Visibility visibility) {
		return visibility == CommunityGroup.Visibility.PUBLIC
				? CommunityGroup.JoinPolicy.OPEN : CommunityGroup.JoinPolicy.INVITE_ONLY;
	}

	private void validateJoinPolicy(CommunityGroup.Visibility visibility, CommunityGroup.JoinPolicy joinPolicy) {
		if (visibility == CommunityGroup.Visibility.PUBLIC && joinPolicy != CommunityGroup.JoinPolicy.OPEN) {
			throw ApiException.badRequest("Public groups must allow anyone to follow.");
		}
		if (visibility == CommunityGroup.Visibility.PRIVATE && joinPolicy == CommunityGroup.JoinPolicy.OPEN) {
			throw ApiException.badRequest("Private groups must use request-to-join or invite-only access.");
		}
	}

	private String cleanTopics(List<String> topics) {
		if (topics == null) return null;
		String normalized = topics.stream().map(String::trim).filter(topic -> !topic.isBlank())
				.map(topic -> topic.replace(",", ""))
				.distinct().collect(Collectors.joining(","));
		if (normalized.length() > 500) throw ApiException.badRequest("Group topics are too long.");
		return normalized.isBlank() ? null : normalized;
	}

	private List<String> splitTopics(String topics) {
		if (topics == null || topics.isBlank()) return List.of();
		return List.of(topics.split(","));
	}

	private PostView postView(GroupPost post) {
		List<GroupPostComment> allComments = postComments.findByGroupPostIdOrderByCreatedAtAsc(post.getId());
		List<CommentView> comments = allComments.stream().filter(comment -> comment.getParent() == null)
				.map(comment -> commentView(comment, allComments)).toList();
		return new PostView(post.getId(), post.getTitle(), post.getBody(), post.getAuthor().getId(),
				post.getAuthor().getDisplayName(), post.getAuthor().getHandle(), post.getCreatedAt(),
				post.getGroup().getId(), post.getGroup().getName(), trailRef(post.getTrail()), allComments.size(), comments);
	}

	private CommentView commentView(GroupPostComment comment, List<GroupPostComment> allComments) {
		List<CommentView> replies = allComments.stream()
				.filter(reply -> reply.getParent() != null && reply.getParent().getId().equals(comment.getId()))
				.map(reply -> new CommentView(reply.getId(), reply.getBody(), reply.getAuthor().getId(),
						reply.getAuthor().getDisplayName(), reply.getAuthor().getHandle(), reply.getCreatedAt(), List.of()))
				.toList();
		return new CommentView(comment.getId(), comment.getBody(), comment.getAuthor().getId(),
				comment.getAuthor().getDisplayName(), comment.getAuthor().getHandle(), comment.getCreatedAt(), replies);
	}

	private StoryTrail resolveTrail(String title) {
		if (title == null || title.isBlank()) return null;
		String cleaned = title.trim();
		return trails.findByTitleIgnoreCase(cleaned).orElseGet(() -> trails.save(StoryTrail.builder()
				.title(cleaned).slug(uniqueTrailSlug(cleaned))
				.description("Follow the latest reporting and community discussion about " + cleaned + ".").build()));
	}

	private String uniqueTrailSlug(String title) {
		String base = slug(title);
		String candidate = base;
		int suffix = 2;
		while (trails.existsBySlug(candidate)) candidate = base + "-" + suffix++;
		return candidate;
	}

	private StoryTrailRefResponse trailRef(StoryTrail trail) {
		return trail == null ? null : new StoryTrailRefResponse(trail.getId(), trail.getTitle(), trail.getSlug());
	}

	private String slug(String name) {
		String base = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
		if (base.isBlank()) base = "group";
		String candidate = base;
		int suffix = 2;
		while (groups.existsBySlug(candidate)) candidate = base + "-" + suffix++;
		return candidate;
	}

	private String clean(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
