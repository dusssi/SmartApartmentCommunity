package com.apartment.community.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-instance fixed-window abuse guard. A multi-replica deployment still needs a shared edge
 * limiter; this filter neither claims nor replaces distributed rate limiting.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_SECONDS = 60;
    private static final int AUTH_LIMIT = 10;
    private static final int API_LIMIT = 120;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requests = new AtomicLong();
    private final SecurityErrorWriter errors;

    public RateLimitFilter(SecurityErrorWriter errors) {
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        boolean auth = request.getRequestURI().startsWith("/api/v1/auth/")
                && !request.getRequestURI().equals("/api/v1/auth/me");
        int limit = auth ? AUTH_LIMIT : API_LIMIT;
        String identity = auth ? request.getRemoteAddr() : authenticatedIdentity(request);
        String key = auth ? "auth:" + identity + ":" + request.getRequestURI() : "api:" + identity;
        long now = Instant.now().getEpochSecond();
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAt >= WINDOW_SECONDS) return new Window(now);
            current.count++;
            return current;
        });
        if (window.count > limit) {
            long retryAfter = Math.max(1, WINDOW_SECONDS - (now - window.startedAt));
            response.setHeader("Retry-After", Long.toString(retryAfter));
            errors.write(request, response, 429, "RATE_LIMITED",
                    "Too many requests. Retry after the indicated interval.");
            return;
        }
        if (requests.incrementAndGet() % 1000 == 0) removeExpired(now);
        chain.doFilter(request, response);
    }

    public void reset() {
        windows.clear();
        requests.set(0);
    }

    private String authenticatedIdentity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.userId();
        }
        return request.getRemoteAddr();
    }

    private void removeExpired(long now) {
        for (Map.Entry<String, Window> entry : windows.entrySet()) {
            if (now - entry.getValue().startedAt > WINDOW_SECONDS * 2) windows.remove(entry.getKey(), entry.getValue());
        }
    }

    private static final class Window {
        private final long startedAt;
        private volatile long count = 1;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
