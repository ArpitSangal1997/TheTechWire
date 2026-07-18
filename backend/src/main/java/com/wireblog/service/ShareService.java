package com.wireblog.service;

import com.wireblog.exception.ApiException;
import com.wireblog.dto.ShareRequest;
import com.wireblog.model.Post;
import com.wireblog.model.Share;
import com.wireblog.model.User;
import com.wireblog.repository.PostRepository;
import com.wireblog.repository.ShareRepository;
import com.wireblog.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShareService {

    private final ShareRepository shareRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CurrentUserResolver currentUserResolver;
    private final NotificationService notificationService;

    public ShareService(ShareRepository shareRepository, PostRepository postRepository,
                         UserRepository userRepository, CurrentUserResolver currentUserResolver,
                         NotificationService notificationService) {
        this.shareRepository = shareRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.currentUserResolver = currentUserResolver;
        this.notificationService = notificationService;
    }

    @Transactional
    public void share(Long postId, ShareRequest req) {
        User sharer = currentUserResolver.requireCurrentUser();
        Post post = postRepository.findById(postId).orElseThrow(() -> ApiException.notFound("Post not found."));

        Share.Channel channel;
        try {
            channel = Share.Channel.valueOf(req.channel().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Unknown share channel: " + req.channel());
        }

        User sharedTo = null;
        if (channel == Share.Channel.IN_APP) {
            if (req.sharedToHandle() == null || req.sharedToHandle().isBlank()) {
                throw ApiException.badRequest("Pick a person to share this post with.");
            }
            sharedTo = userRepository.findByHandle(req.sharedToHandle().toLowerCase())
                    .orElseThrow(() -> ApiException.notFound("No user found with that handle."));
        }

        Share share = Share.builder()
                .post(post)
                .sharedBy(sharer)
                .sharedTo(sharedTo)
                .channel(channel)
                .build();
        shareRepository.save(share);

        notificationService.notifyShare(post, sharedTo, sharer);

        post.setShareCount(post.getShareCount() + 1);
        postRepository.save(post);
    }
}
