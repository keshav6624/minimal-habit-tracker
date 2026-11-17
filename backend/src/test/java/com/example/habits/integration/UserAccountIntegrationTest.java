package com.example.habits.integration;

import com.example.habits.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Settings GET/PUT validation + account deletion with password re-verification. */
class UserAccountIntegrationTest extends AbstractIntegrationTest {

    @Test
    void default_settings_then_update() throws Exception {
        MockHttpSession session = register(uniqueEmail("set"), STRONG_PASSWORD, "S",
                "Asia/Kolkata");

        mockMvc.perform(get("/api/v1/users/me/settings").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"))
                .andExpect(jsonPath("$.weekStartsOn").value(1))
                .andExpect(jsonPath("$.remindersEnabled").value(false));

        mockMvc.perform(put("/api/v1/users/me/settings")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"timezone":"Europe/Berlin","weekStartsOn":7,
                                 "remindersEnabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Europe/Berlin"))
                .andExpect(jsonPath("$.weekStartsOn").value(7))
                .andExpect(jsonPath("$.remindersEnabled").value(true));
    }

    @Test
    void invalid_settings_are_400() throws Exception {
        MockHttpSession session = register(uniqueEmail("set2"), STRONG_PASSWORD, "S", "UTC");

        mockMvc.perform(put("/api/v1/users/me/settings")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timezone\":\"Not/AZone\",\"weekStartsOn\":1,"
                                + "\"remindersEnabled\":false}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/users/me/settings")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timezone\":\"UTC\",\"weekStartsOn\":8,"
                                + "\"remindersEnabled\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_account_wrong_password_401_right_password_204() throws Exception {
        String email = uniqueEmail("del");
        MockHttpSession session = register(email, STRONG_PASSWORD, "D", "UTC");

        // Wrong password -> 401 INVALID_CREDENTIALS, account intact.
        mockMvc.perform(delete("/api/v1/users/me")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Wr0ngPassw0rd!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type")
                        .value(com.example.habits.exception.ErrorType.INVALID_CREDENTIALS.name()));
        mockMvc.perform(get("/api/v1/users/me").session(session))
                .andExpect(status().isOk());

        // Right password -> 204, session invalidated, login no longer works.
        mockMvc.perform(delete("/api/v1/users/me")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + STRONG_PASSWORD + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me").session(session))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}"
                                .formatted(email, STRONG_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_account_without_body_is_400() throws Exception {
        MockHttpSession session = register(uniqueEmail("del2"), STRONG_PASSWORD, "D", "UTC");
        mockMvc.perform(delete("/api/v1/users/me")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isBadRequest());
    }
}
