package com.example.habits.integration;

import com.example.habits.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** User B can never touch User A's habit — always 404, never 403. */
class CrossTenantIntegrationTest extends AbstractIntegrationTest {

    private UUID createHabitFor(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/habits")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice habit","scheduleType":"DAILY","startDate":"2024-01-01"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonMapper.builder().build()
                .readTree(result.getResponse().getContentAsString())
                .get("id").asText());
    }

    @Test
    void foreign_habit_is_404_for_every_verb() throws Exception {
        MockHttpSession alice = register(uniqueEmail("alice"), STRONG_PASSWORD, "A", "UTC");
        MockHttpSession bob = register(uniqueEmail("bob"), STRONG_PASSWORD, "B", "UTC");
        UUID aliceHabit = createHabitFor(alice);
        String base = "/api/v1/habits/" + aliceHabit;

        mockMvc.perform(get(base).session(bob)).andExpect(status().isNotFound());
        mockMvc.perform(patch(base).with(csrf()).session(bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(base + "/completions/2024-01-05").with(csrf()).session(bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(base + "/completions/2024-01-05").with(csrf()).session(bob))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(base + "/archive").with(csrf()).session(bob))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(base + "/restore").with(csrf()).session(bob))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(base).with(csrf()).session(bob))
                .andExpect(status().isNotFound());

        // Alice's habit is untouched by all of Bob's attempts.
        mockMvc.perform(get(base).session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice habit"))
                .andExpect(jsonPath("$.archivedAt").doesNotExist());
    }

    @Test
    void foreign_habit_does_not_appear_in_bobs_listing() throws Exception {
        MockHttpSession alice = register(uniqueEmail("alice2"), STRONG_PASSWORD, "A", "UTC");
        createHabitFor(alice);
        MockHttpSession bob = register(uniqueEmail("bob2"), STRONG_PASSWORD, "B", "UTC");

        mockMvc.perform(get("/api/v1/habits").session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
