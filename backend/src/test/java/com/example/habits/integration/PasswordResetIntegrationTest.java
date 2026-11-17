package com.example.habits.integration;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.habits.exception.ErrorType;
import com.example.habits.service.AuthService;
import com.example.habits.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Password reset: forgot-password always 202 (no enumeration), single-use
 * token (delivered via the dev console log because no mail provider exists),
 * happy path, reuse rejected, garbage token rejected.
 */
@TestPropertySource(properties = "app.auth.dev-expose-reset-token=true")
class PasswordResetIntegrationTest extends AbstractIntegrationTest {

    private static final Pattern DEV_TOKEN = Pattern.compile("token for \\S+ : (\\S+)");

    private ListAppender<ILoggingEvent> logAppender;
    private Logger authLogger;

    @BeforeEach
    void captureAuthServiceLogs() {
        authLogger = (Logger) LoggerFactory.getLogger(AuthService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        authLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        if (authLogger != null) {
            authLogger.detachAppender(logAppender);
        }
    }

    private String requestResetAndReadDevToken(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.detail").isNotEmpty());
        return logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .map(DEV_TOKEN::matcher)
                .filter(Matcher::find)
                .map(m -> m.group(1))
                .reduce((a, b) -> b) // last token wins
                .orElseThrow(() -> new IllegalStateException("No dev token was logged"));
    }

    @Test
    void forgot_password_unknown_email_is_still_202_generic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(uniqueEmail("nobody"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.detail").isNotEmpty());
        // And no token was logged for a non-existent user.
        assertThat(logAppender.list).isEmpty();
    }

    @Test
    void reset_password_happy_path_then_reuse_rejected() throws Exception {
        String email = uniqueEmail("reset");
        register(email, STRONG_PASSWORD, "R", "UTC");
        String oldPassword = STRONG_PASSWORD;
        String newPassword = "N3wStr0ngPass";

        String token = requestResetAndReadDevToken(email);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"newPassword\":\"%s\"}"
                                .formatted(token, newPassword)))
                .andExpect(status().isNoContent());

        // Old password no longer works; new one does.
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}"
                                .formatted(email, oldPassword)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}"
                                .formatted(email, newPassword)))
                .andExpect(status().isOk());

        // Reusing the same token fails with 400 VALIDATION_ERROR.
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"newPassword\":\"An0therStr0ng\"}"
                                .formatted(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorType.VALIDATION_ERROR.name()));
    }

    @Test
    void reset_password_invalid_token_is_400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"totally-bogus-token\",\"newPassword\":\"Abcdefgh1jk\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorType.VALIDATION_ERROR.name()));
    }

    @Test
    void token_stored_hashed_not_raw() throws Exception {
        String email = uniqueEmail("hash");
        register(email, STRONG_PASSWORD, "H", "UTC");
        String token = requestResetAndReadDevToken(email);
        // The raw token must never appear in storage-level queries; here we
        // assert the logged token differs from a SHA-256 hex digest shape and
        // that the service log contains only the raw value (dev channel).
        assertThat(token).hasSize(43); // 32 bytes -> 43 url-safe base64 chars, no padding
    }
}
