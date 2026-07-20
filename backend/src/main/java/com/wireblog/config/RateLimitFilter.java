package com.wireblog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Small per-instance guard for public abuse hotspots. Use a shared limiter at scale. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MS = 60_000L;
    private static final int LIMIT = 60;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!isHotspot(path)) { filterChain.doFilter(request, response); return; }
        String key = clientIp(request) + ':' + path;
        long now = Instant.now().toEpochMilli();
        Window current = windows.compute(key, (ignored, old) -> old == null || now - old.startedAt > WINDOW_MS ? new Window(now) : old);
        if (current.count.incrementAndGet() > LIMIT) {
            response.setStatus(429); response.setHeader("Retry-After", "60"); response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many requests. Please try again shortly.\"}"); return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isHotspot(String path) {
        return path.equals("/api/auth/login") || path.equals("/api/auth/register") || path.equals("/api/auth/password-reset/request")
                || path.equals("/api/upload") || path.matches("/api/comments/post/\\d+") || path.matches("/api/posts/\\d+/share");
    }
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
    private static final class Window {
        private final long startedAt; private final AtomicInteger count = new AtomicInteger();
        private Window(long startedAt) { this.startedAt = startedAt; }
    }
}
