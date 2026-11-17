package com.example.habits.integration;

import com.example.habits.exception.ErrorType;
import com.example.habits.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Habit CRUD, validation, archive/restore semantics, hard delete. */
class HabitCrudIntegrationTest extends AbstractIntegrationTest {

    private MvcResult createHabit(MockHttpSession session, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/habits")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String habitJson(UUID id) {
        return "/api/v1/habits/" + id;
    }

    @Test
    void create_get_patch_delete_lifecycle() throws Exception {
        MockHttpSession session = register(uniqueEmail("crud"), STRONG_PASSWORD, "C", "UTC");

        MvcResult created = createHabit(session, """
                {"name":"Morning run","description":"5km","color":"#0f0","icon":"runner",
                 "scheduleType":"DAILY","scheduleDays":[],"startDate":"2024-01-01"}
                """);
        JsonNode habit = JsonMapper.builder().build()
                .readTree(created.getResponse().getContentAsString());
        UUID id = UUID.fromString(habit.get("id").asText());

        assertThat(habit.get("scheduleType").asText()).isEqualTo("DAILY");
        assertThat(habit.get("scheduleDays").isEmpty()).isTrue();
        // archivedAt is null and nulls are omitted from the payload (NON_NULL).
        assertThat(habit.has("archivedAt")).isFalse();

        // GET returns the same DTO.
        mockMvc.perform(get(habitJson(id)).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Morning run"));

        // PATCH: only name changes; nulls are unchanged.
        mockMvc.perform(patch(habitJson(id))
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Evening run\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Evening run"))
                .andExpect(jsonPath("$.description").value("5km"))
                .andExpect(jsonPath("$.color").value("#0f0"));

        // PATCH: empty string clears description.
        mockMvc.perform(patch(habitJson(id))
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").doesNotExist());

        // DELETE is a hard delete.
        mockMvc.perform(delete(habitJson(id)).with(csrf()).session(session))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(habitJson(id)).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void custom_schedule_validation_rules() throws Exception {
        MockHttpSession session = register(uniqueEmail("sched"), STRONG_PASSWORD, "S", "UTC");

        // CUSTOM requires a non-empty subset of 1..7.
        mockMvc.perform(post("/api/v1/habits")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Custom","scheduleType":"CUSTOM","scheduleDays":[],
                                 "startDate":"2024-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("scheduleDays"));

        mockMvc.perform(post("/api/v1/habits")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Custom","scheduleType":"CUSTOM","scheduleDays":[1,8],
                                 "startDate":"2024-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("scheduleDays"));

        // DAILY/WEEKDAYS must have empty scheduleDays.
        mockMvc.perform(post("/api/v1/habits")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Daily","scheduleType":"DAILY","scheduleDays":[1],
                                 "startDate":"2024-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("scheduleDays"));

        // Valid CUSTOM round-trips through the converter.
        MvcResult result = createHabit(session, """
                {"name":"Custom ok","scheduleType":"CUSTOM","scheduleDays":[1,3,7],
                 "startDate":"2024-01-01"}
                """);
        JsonNode habit = JsonMapper.builder().build()
                .readTree(result.getResponse().getContentAsString());
        assertThat(habit.get("scheduleDays").toString()).isEqualTo("[1,3,7]");
    }

    @Test
    void list_pagination_and_status_filter() throws Exception {
        MockHttpSession session = register(uniqueEmail("page"), STRONG_PASSWORD, "P", "UTC");

        createHabit(session, """
                {"name":"One","scheduleType":"DAILY","startDate":"2024-01-01"}
                """);
        MvcResult second = createHabit(session, """
                {"name":"Two","scheduleType":"DAILY","startDate":"2024-01-01"}
                """);
        UUID twoId = UUID.fromString(JsonMapper.builder().build()
                .readTree(second.getResponse().getContentAsString())
                .get("id").asText());

        mockMvc.perform(get("/api/v1/habits").session(session)
                        .queryParam("page", "0").queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        // Archive the second one; the default (active) list no longer shows it.
        mockMvc.perform(post(habitJson(twoId) + "/archive").with(csrf()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/habits").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/habits?status=archived").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(twoId.toString()));

        // Invalid status enum -> 400.
        mockMvc.perform(get("/api/v1/habits?status=bogus").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorType.VALIDATION_ERROR.name()));
    }

    @Test
    void restore_clears_archived_at() throws Exception {
        MockHttpSession session = register(uniqueEmail("arch"), STRONG_PASSWORD, "A", "UTC");
        MvcResult created = createHabit(session, """
                {"name":"Archive me","scheduleType":"WEEKDAYS","startDate":"2024-01-01"}
                """);
        UUID id = UUID.fromString(JsonMapper.builder().build()
                .readTree(created.getResponse().getContentAsString())
                .get("id").asText());

        mockMvc.perform(post(habitJson(id) + "/archive").with(csrf()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedAt").isNotEmpty());

        mockMvc.perform(post(habitJson(id) + "/restore").with(csrf()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedAt").doesNotExist());
    }

    @Test
    void size_over_100_is_rejected() throws Exception {
        MockHttpSession session = register(uniqueEmail("size"), STRONG_PASSWORD, "Z", "UTC");
        mockMvc.perform(get("/api/v1/habits?size=101").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorType.VALIDATION_ERROR.name()));
    }
}
