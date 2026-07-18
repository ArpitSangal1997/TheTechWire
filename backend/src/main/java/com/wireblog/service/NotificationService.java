package com.wireblog.service;

import com.wireblog.model.Notification;
import com.wireblog.model.Post;
import com.wireblog.model.User;
import com.wireblog.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notifyUser(User user, String type, String message, String link) {
        if (user == null) return;
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .link(link)
                .build();
        notificationRepository.save(notification);
    }

    public List<Notification> listFor(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void markAllRead(User user) {
        List<Notification> items = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        items.forEach(item -> item.setRead(true));
        notificationRepository.saveAll(items);
    }

    public long unreadCount(User user) {
        return listFor(user).stream().filter(item -> !item.isRead()).count();
    }

    public void notifyShare(Post post, User sharedTo, User sharer) {
        if (sharedTo == null || sharer == null || sharedTo.getId().equals(sharer.getId())) return;
        notifyUser(sharedTo, "SHARE", sharer.getDisplayName() + " shared \"" + post.getTitle() + "\" with you.", "/post/" + post.getSlug());
    }

    public void notifyReply(User author, Post post, User replier) {
        if (author == null || replier == null || author.getId().equals(replier.getId())) return;
        notifyUser(author, "REPLY", replier.getDisplayName() + " replied to your comment on \"" + post.getTitle() + "\".", "/post/" + post.getSlug());
    }
}
