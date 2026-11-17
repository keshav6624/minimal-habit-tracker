package com.example.habits.integration;

import com.example.habits.exception.ErrorType;
import com.example.habits.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * In-memory fixed-window rate limit: 11th POST /api/v1/auth/login within a
 * minute from the same IP -> 429 RATE_LIMITED envelope. (Single-node MVP
 * limitation — see RateLimitFilter.)
 */
class RateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final String BODY =
            "{\"email\":\"someone@example.com\",\"password\":\"Whatever123\"}";

    @Test
    void eleventh_login_within_window_is_429() throws Exception {
        // First 10 requests pass the limiter (they 401 — unknown user).
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(BODY))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type")
                            .value(ErrorType.INVALID_CREDENTIALS.name()));
        }
        // Request 11 -> 429 envelope from the filter itself.
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value(ErrorType.RATE_LIMITED.name()))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void other_auth_endpoints_are_limited_independently() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"x@example.com\"}"))
                    .andExpect(status().isAccepted());
        }
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@example.com\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value(ErrorType.RATE_LIMITED.name()));
    }
}
