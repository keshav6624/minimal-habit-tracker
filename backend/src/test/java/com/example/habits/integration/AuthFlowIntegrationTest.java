package com.example.habits.integration;

import com.example.habits.exception.ErrorType;
import com.example.habits.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Register → login → me → logout with the REAL CSRF cookie flow, plus
 * duplicate-email 409 and identical 401s for unknown email / wrong password.
 */
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void register_me_logout_flow_with_real_csrf_cookie() throws Exception {
        String email = uniqueEmail("flow");
        String password = STRONG_PASSWORD;

        // 1. GET /api/v1/auth/csrf sets the XSRF-TOKEN cookie.
        CsrfFlow csrf = fetchCsrf();

        // 2. Register echoing the raw cookie value in X-XSRF-TOKEN (a browser
        // would also send the XSRF-TOKEN cookie itself).
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", csrf.token()))
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","displayName":"Flow","timezone":"Asia/Kolkata"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("Flow"))
                .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"))
                .andReturn();

        MockHttpSession session = (MockHttpSession)
                registerResult.getRequest().getSession(false);

        // 3. The session is authenticated: GET /users/me works.
        mockMvc.perform(get("/api/v1/users/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // 4. Password stored hashed: never echoed back.
        mockMvc.perform(get("/api/v1/users/me").session(session))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        // 5. Logout invalidates the session.
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ErrorType.UNAUTHORIZED.name()));
    }

    @Test
    void duplicate_register_returns_409_email_already_exists() throws Exception {
        String email = uniqueEmail("dup");
        register(email, STRONG_PASSWORD, "First", "UTC");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","timezone":"UTC"}
                                """.formatted(email.toUpperCase(), STRONG_PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ErrorType.EMAIL_ALREADY_EXISTS.name()));
    }

    @Test
    void wrong_password_and_unknown_email_return_identical_401_shape() throws Exception {
        String email = uniqueEmail("creds");
        register(email, STRONG_PASSWORD, "A", "UTC");

        String wrongPasswordBody = """
                {"email":"%s","password":"Wr0ngPassw0rd!"}
                """.formatted(email);
        String unknownEmailBody = """
                {"email":"%s","password":"Str0ngPassw0rd"}
                """.formatted(uniqueEmail("ghost"));

        String wrongPw = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongPasswordBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ErrorType.INVALID_CREDENTIALS.name()))
                .andReturn().getResponse().getContentAsString();

        String unknownEmail = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unknownEmailBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ErrorType.INVALID_CREDENTIALS.name()))
                .andReturn().getResponse().getContentAsString();

        // Same status + same type for both failure modes (no enumeration).
        org.assertj.core.api.Assertions.assertThat(wrongPasswordOnlyTypeRemoved(wrongPw))
                .isEqualTo(wrongPasswordOnlyTypeRemoved(unknownEmail));
    }

    private String wrongPasswordOnlyTypeRemoved(String body) {
        return body.replace(ErrorType.INVALID_CREDENTIALS.name(), "SAME")
                .replaceAll("\"detail\":\"[^\"]*\"", "\"detail\":\"X\"")
                .replaceAll("\"traceId\":\"[^\"]*\"", "\"traceId\":\"X\"");
    }

    @Test
    void login_success_returns_user_dto_and_session() throws Exception {
        String email = uniqueEmail("login");
        register(email, STRONG_PASSWORD, "L", "Asia/Kolkata");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, STRONG_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void protected_endpoint_without_session_is_401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ErrorType.UNAUTHORIZED.name()));
    }

    @Test
    void register_normalizes_email_to_lowercase() throws Exception {
        String email = uniqueEmail("Normalize");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","timezone":"UTC"}
                                """.formatted(email, STRONG_PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        var json = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(result.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(json.get("email").asText())
                .isEqualTo(email.toLowerCase());
    }
}
