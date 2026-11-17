package com.example.habits.integration;

import com.example.habits.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Export JSON + CSV: attachment disposition, proper CSV escaping, caller-scoped. */
class ExportIntegrationTest extends AbstractIntegrationTest {

    private record Setup(MockHttpSession session, UUID userId, UUID habitId,
                         List<String> dates) {}

    private Setup seedUser(String tag, String habitName) throws Exception {
        MockHttpSession session = register(uniqueEmail("exp" + tag), STRONG_PASSWORD,
                "Exporter " + tag, "UTC");
        MvcResult created = mockMvc.perform(post("/api/v1/habits")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","scheduleType":"DAILY","startDate":"2024-01-01"}
                                """.formatted(habitName)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID habitId = UUID.fromString(JsonMapper.builder().build()
                .readTree(created.getResponse().getContentAsString())
                .get("id").asText());
        List<String> dates = List.of("2024-01-02", "2024-01-03");
        for (String date : dates) {
            mockMvc.perform(put("/api/v1/habits/" + habitId + "/completions/" + date)
                            .with(csrf())
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"completed\":true}"))
                    .andExpect(status().isOk());
        }
        String userId = JsonMapper.builder().build()
                .readTree(mockMvc.perform(get("/api/v1/users/me").session(session))
                        .andReturn().getResponse().getContentAsString())
                .get("id").asText();
        return new Setup(session, UUID.fromString(userId), habitId, dates);
    }

    @Test
    void json_export_contains_only_callers_data() throws Exception {
        Setup alice = seedUser("A", "Run");
        seedUser("B", "Meditate"); // Bob's data must NOT leak into Alice's export

        MvcResult result = mockMvc.perform(get("/api/v1/users/me/export?format=json")
                        .session(alice.session()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"habit-tracker-export.json\""))
                .andReturn();

        JsonNode json = JsonMapper.builder().build()
                .readTree(result.getResponse().getContentAsString());
        assertThat(json.get("user").get("id").asText()).isEqualTo(alice.userId().toString());
        assertThat(json.get("settings").get("weekStartsOn").asInt()).isEqualTo(1);
        assertThat(json.get("habits")).hasSize(1);
        assertThat(json.get("habits").get(0).get("name").asText()).isEqualTo("Run");
        assertThat(json.get("completions")).hasSize(alice.dates().size());
        assertThat(json.get("completions").get(0).get("localDate").asText())
                .isEqualTo("2024-01-02");
        assertThat(json.get("completions").get(0).get("completed").asBoolean()).isTrue();
        assertThat(json.get("completions").get(0).get("completedAt")).isNotNull();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("Meditate");
    }

    @Test
    void csv_export_has_header_rows_and_proper_escaping() throws Exception {
        // Habit name containing comma, quote and spaces exercises RFC 4180 quoting.
        MockHttpSession session = register(uniqueEmail("csv"), STRONG_PASSWORD, "C", "UTC");
        MvcResult created = mockMvc.perform(post("/api/v1/habits")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Say \\"hi\\", now","scheduleType":"DAILY",
                                 "startDate":"2024-01-01"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID habitId = UUID.fromString(JsonMapper.builder().build()
                .readTree(created.getResponse().getContentAsString())
                .get("id").asText());
        mockMvc.perform(put("/api/v1/habits/" + habitId + "/completions/2024-02-01")
                        .with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/users/me/export?format=csv")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"habit-tracker-export.csv\""))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        String[] lines = csv.split("\\r?\\n");
        assertThat(lines[0]).isEqualTo("habit_id,habit_name,local_date,completed,completed_at");
        // The name field is quoted and internal quotes doubled.
        assertThat(lines[1]).contains(habitId.toString(),
                "\"Say \"\"hi\"\", now\"", "2024-02-01", "true");
        assertThat(lines).hasSize(2); // header + exactly one data row
    }

    @Test
    void invalid_format_is_400() throws Exception {
        MockHttpSession session = register(uniqueEmail("fmt"), STRONG_PASSWORD, "F", "UTC");
        mockMvc.perform(get("/api/v1/users/me/export?format=xml").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("format"));
    }
}
