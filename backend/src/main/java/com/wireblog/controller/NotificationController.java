package com.wireblog.controller;

import com.wireblog.dto.NotificationResponse;
import com.wireblog.model.User;
import com.wireblog.service.CurrentUserResolver;
import com.wireblog.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserResolver currentUserResolver;

    public NotificationController(NotificationService notificationService, CurrentUserResolver currentUserResolver) {
        this.notificationService = notificationService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        User user = currentUserResolver.requireCurrentUser();
        return notificationService.listFor(user).stream()
                .map(item -> new NotificationResponse(item.getId(), item.getType(), item.getMessage(), item.getLink(),
                        item.isRead(), item.getCreatedAt()))
                .toList();
    }

    @GetMapping("/unread-count")
    public long unreadCount() {
        return notificationService.unreadCount(currentUserResolver.requireCurrentUser());
    }

    @PatchMapping("/read-all")
    public void markAllRead() {
        notificationService.markAllRead(currentUserResolver.requireCurrentUser());
    }
}
