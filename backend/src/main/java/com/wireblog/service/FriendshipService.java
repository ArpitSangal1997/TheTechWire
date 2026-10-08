package com.wireblog.service;

import com.wireblog.dto.FriendshipDtos.FriendView;
import com.wireblog.dto.FriendshipDtos.UserView;
import com.wireblog.exception.ApiException;
import com.wireblog.model.Friendship;
import com.wireblog.model.User;
import com.wireblog.repository.FriendshipRepository;
import com.wireblog.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class FriendshipService {
    private final FriendshipRepository friendships;
    private final UserRepository users;
    private final CurrentUserResolver current;

    public FriendshipService(FriendshipRepository friendships, UserRepository users, CurrentUserResolver current) {
        this.friendships = friendships;
        this.users = users;
        this.current = current;
    }

    @Transactional(readOnly = true)
    public List<FriendView> list() {
        User me = current.requireCurrentUser();
        return friendships.findByRequesterIdOrRecipientId(me.getId(), me.getId()).stream()
                .map(friendship -> {
                    User other = friendship.getRequester().getId().equals(me.getId())
                            ? friendship.getRecipient() : friendship.getRequester();
                    return new FriendView(other.getId(), other.getDisplayName(), other.getHandle(), other.getAvatarUrl(),
                            relationship(friendship, me), friendship.getCreatedAt());
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<UserView> search(String query) {
        User me = current.requireCurrentUser();
        if (query == null || query.trim().length() < 2) return List.of();
        String normalized = query.trim();
        return users.searchActiveUsers(normalized, me.getId(), PageRequest.of(0, 20, Sort.by("displayName").ascending()))
                .stream().map(user -> {
                    String status = friendships.findBetween(me.getId(), user.getId())
                            .map(friendship -> relationship(friendship, me)).orElse("NONE");
                    return new UserView(user.getId(), user.getDisplayName(), user.getHandle(), user.getAvatarUrl(), status);
                }).toList();
    }

    @Transactional
    public UserView request(Long userId) {
        User me = current.requireCurrentUser();
        User other = findTarget(userId, me);
        Friendship existing = friendships.findBetween(me.getId(), other.getId()).orElse(null);
        if (existing == null) {
            existing = friendships.save(Friendship.builder().requester(me).recipient(other)
                    .status(Friendship.Status.REQUESTED).build());
        } else if (existing.getStatus() == Friendship.Status.REQUESTED
                && existing.getRequester().getId().equals(other.getId())) {
            existing.setStatus(Friendship.Status.ACCEPTED);
            existing.setAcceptedAt(Instant.now());
        } else if (existing.getStatus() == Friendship.Status.ACCEPTED) {
            throw ApiException.conflict("You are already friends.");
        } else {
            throw ApiException.conflict("A friend request is already pending.");
        }
        return toUserView(other, relationship(existing, me));
    }

    @Transactional
    public UserView accept(Long userId) {
        User me = current.requireCurrentUser();
        User other = findTarget(userId, me);
        Friendship friendship = friendships.findBetween(me.getId(), other.getId())
                .orElseThrow(() -> ApiException.notFound("Friend request not found."));
        if (friendship.getStatus() != Friendship.Status.REQUESTED
                || !friendship.getRecipient().getId().equals(me.getId())) {
            throw ApiException.badRequest("There is no incoming friend request to accept.");
        }
        friendship.setStatus(Friendship.Status.ACCEPTED);
        friendship.setAcceptedAt(Instant.now());
        return toUserView(other, "FRIEND");
    }

    @Transactional
    public void remove(Long userId) {
        User me = current.requireCurrentUser();
        User other = findTarget(userId, me);
        friendships.findBetween(me.getId(), other.getId()).ifPresent(friendships::delete);
    }

    private User findTarget(Long id, User me) {
        if (me.getId().equals(id)) throw ApiException.badRequest("You cannot add yourself as a friend.");
        return users.findById(id).filter(User::isEnabled)
                .orElseThrow(() -> ApiException.notFound("User not found."));
    }

    private String relationship(Friendship friendship, User me) {
        if (friendship.getStatus() == Friendship.Status.ACCEPTED) return "FRIEND";
        return friendship.getRequester().getId().equals(me.getId()) ? "OUTGOING_REQUEST" : "INCOMING_REQUEST";
    }

    private UserView toUserView(User user, String relationship) {
        return new UserView(user.getId(), user.getDisplayName(), user.getHandle(), user.getAvatarUrl(), relationship);
    }
}
