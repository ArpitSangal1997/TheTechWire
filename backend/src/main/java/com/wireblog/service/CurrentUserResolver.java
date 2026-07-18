package com.wireblog.service;

import com.wireblog.exception.ApiException;
import com.wireblog.model.User;
import com.wireblog.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {
    private final UserRepository userRepository;

    public CurrentUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw ApiException.forbidden("You need to be signed in to do that.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> ApiException.forbidden("Your session is no longer valid - please sign in again."));
    }
}
