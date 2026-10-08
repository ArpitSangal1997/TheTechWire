package com.wireblog.controller;

import com.wireblog.dto.FriendshipDtos.FriendView;
import com.wireblog.dto.FriendshipDtos.UserView;
import com.wireblog.service.FriendshipService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {
    private final FriendshipService friendships;

    public FriendshipController(FriendshipService friendships) {
        this.friendships = friendships;
    }

    @GetMapping
    public List<FriendView> list() {
        return friendships.list();
    }

    @GetMapping("/search")
    public List<UserView> search(@RequestParam(defaultValue = "") String q) {
        return friendships.search(q);
    }

    @PostMapping("/{userId}")
    public UserView request(@PathVariable Long userId) {
        return friendships.request(userId);
    }

    @PostMapping("/{userId}/accept")
    public UserView accept(@PathVariable Long userId) {
        return friendships.accept(userId);
    }

    @DeleteMapping("/{userId}")
    public void remove(@PathVariable Long userId) {
        friendships.remove(userId);
    }
}
