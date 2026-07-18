package com.wireblog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public void sendVerificationEmail(String email, String token) {
        log.info("Development verification link for {}: {}/verify-email?token={}", email, frontendUrl, token);
    }

    public void sendPasswordResetEmail(String email, String token) {
        log.info("Development password reset link for {}: {}/reset-password?token={}", email, frontendUrl, token);
    }
}
