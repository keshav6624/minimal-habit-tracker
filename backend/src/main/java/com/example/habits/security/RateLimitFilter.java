package com.example.habits.security;

import com.example.habits.exception.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed-window in-memory rate limiter for auth endpoints (10 req/min per
 * IP+endpoint). Exceeding the limit returns a 429 RATE_LIMITED envelope.
 *
 * <p>MVP limitation: state lives in this JVM's memory only, so this protects a
 * single node. Horizontal deployments need a shared store (e.g. Redis) —
 * deliberately out of scope for the MVP (see TECH_STACK.md "no Redis without
 * measured need").</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    static final int MAX_REQUESTS = 10;
    static final long WINDOW_MILLIS = 60_000;

    private static final List<String> PROTECTED_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password");

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Window> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (PROTECTED_PATHS.contains(path)) {
            String key = clientIp(request) + "|" + path;
            Window window = buckets.compute(key,
                    (k, existing) -> (existing == null || existing.isExpired()) ? new Window() : existing);
            if (window.count.incrementAndGet() > MAX_REQUESTS) {
                reject(request, response, path);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response,
                        String path) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String traceId = String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
        var body = new com.example.habits.exception.ErrorResponse(
                ErrorType.RATE_LIMITED,
                ErrorType.RATE_LIMITED.getDefaultTitle(),
                429,
                "Too many requests to " + path + ". Try again in a minute.",
                null,
                traceId);
        objectMapper.writeValue(response.getWriter(), body);
    }

    /** Test hook: clears all windows so @SpringBootTest classes stay isolated. */
    public void reset() {
        buckets.clear();
    }

    /** First hop of X-Forwarded-For when present (framework strategy is enabled), else remote addr. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        private final long startMillis = System.currentTimeMillis();
        private final AtomicLong count = new AtomicLong();

        private boolean isExpired() {
            return System.currentTimeMillis() - startMillis >= WINDOW_MILLIS;
        }
    }
}
