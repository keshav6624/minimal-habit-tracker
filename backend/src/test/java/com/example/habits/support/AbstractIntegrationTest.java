package com.example.habits.support;

import com.example.habits.security.RateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Field;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base for @SpringBootTest integration tests (H2 in PostgreSQL mode).
 * NOTE: Testcontainers-PostgreSQL is the long-term replacement; H2 keeps this
 * runnable on machines without Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected RateLimitFilter rateLimitFilter;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @BeforeEach
    void resetRateLimitAndSecurityContext() {
        rateLimitFilter.reset();
        SecurityContextHolder.clearContext();
        restoreCookieCsrfRepository();
    }

    /**
     * spring-security-test's {@code csrf()} request post-processor replaces the
     * CsrfFilter's token repository on the SHARED, cached FilterChainProxy with
     * a session-backed TestCsrfTokenRepository and never restores it. Tests
     * exercising the real SPA cookie flow (GET /csrf + Set-Cookie) would then
     * find no cookie at all. Re-install the production repository before every
     * test; the post-processor re-poisons per request and keeps working.
     */
    private void restoreCookieCsrfRepository() {
        for (SecurityFilterChain chain : springSecurityFilterChain.getFilterChains()) {
            if (!(chain instanceof DefaultSecurityFilterChain defaultChain)) {
                continue;
            }
            for (var filter : defaultChain.getFilters()) {
                if (!(filter instanceof CsrfFilter csrfFilter)) {
                    continue;
                }
                try {
                    Field repository = CsrfFilter.class.getDeclaredField("tokenRepository");
                    repository.setAccessible(true);
                    if (!(repository.get(csrfFilter) instanceof CookieCsrfTokenRepository)) {
                        repository.set(csrfFilter, CookieCsrfTokenRepository.withHttpOnlyFalse());
                    }
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot restore CSRF cookie repository", e);
                }
            }
        }
    }

    /** Registers a fresh user and returns the authenticated session. */
    protected MockHttpSession register(String email, String password, String displayName,
                                       String timezone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","displayName":"%s","timezone":"%s"}
                                """.formatted(email, password, displayName, timezone)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isCreated())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        if (session == null) {
            throw new IllegalStateException("Register did not create a session");
        }
        return session;
    }

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + java.util.UUID.randomUUID() + "@example.com";
    }

    protected static final String STRONG_PASSWORD = "Str0ngPassw0rd";

    /** The real SPA CSRF flow: fetch the cookie and echo its value. No session needed. */
    protected record CsrfFlow(String token) {}

    protected CsrfFlow fetchCsrf() throws Exception {
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/v1/auth/csrf"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isNoContent())
                .andReturn();
        jakarta.servlet.http.Cookie cookie =
                result.getResponse().getCookie("XSRF-TOKEN");
        if (cookie == null) {
            throw new IllegalStateException("XSRF-TOKEN cookie was not set by /csrf");
        }
        return new CsrfFlow(cookie.getValue());
    }

    /** Session object from a MockMvc result (request-scoped). */
    protected MockHttpSession sessionOf(MvcResult result, HttpServletRequest ignored) {
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
