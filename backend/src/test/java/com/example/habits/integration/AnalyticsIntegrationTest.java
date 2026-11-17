package com.example.habits.integration;

import com.example.habits.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Schedule-aware analytics against fixed historical ranges (deterministic —
 * "today" clamping only ever shrinks the end, and our ranges end in the past).
 */
class AnalyticsIntegrationTest extends AbstractIntegrationTest {

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");

    private MockHttpSession userWithHabit(String name, String scheduleType,
                                          String days, LocalDate start,
                                          String timezone) throws Exception {
        MockHttpSession session = register(uniqueEmail("ana"), STRONG_PASSWORD, name, timezone);
        mockMvc.perform(post("/api/v1/habits")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","scheduleType":"%s","scheduleDays":%s,
                                 "startDate":"%s"}
                                """.formatted(name, scheduleType, days, start)))
                .andExpect(status().isCreated());
        return session;
    }

    private UUID firstHabitId(MockHttpSession session) throws Exception {
        MvcResult list = mockMvc.perform(get("/api/v1/habits").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(JsonMapper.builder().build()
                .readTree(list.getResponse().getContentAsString())
                .get("content").get(0).get("id").asText());
    }

    private void complete(MockHttpSession session, UUID habitId, String date,
                          boolean completed) throws Exception {
        mockMvc.perform(put("/api/v1/habits/" + habitId + "/completions/" + date)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":" + completed + "}"))
                .andExpect(status().isOk());
    }

    @Test
    void daily_habit_summary_counts() throws Exception {
        MockHttpSession session = userWithHabit("Daily", "DAILY", "[]",
                LocalDate.of(2024, 1, 1), "Asia/Kolkata");
        UUID id = firstHabitId(session);

        // Jan 1..5 completed, Jan 6 skipped, Jan 7..10 completed -> 9 of 31.
        for (int d = 1; d <= 5; d++) {
            complete(session, id, LocalDate.of(2024, 1, d).toString(), true);
        }
        for (int d = 7; d <= 10; d++) {
            complete(session, id, LocalDate.of(2024, 1, d).toString(), true);
        }

        mockMvc.perform(get("/api/v1/analytics/summary?from=2024-01-01&to=2024-01-31")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habits.length()").value(1))
                .andExpect(jsonPath("$.habits[0].scheduledCount").value(31))
                .andExpect(jsonPath("$.habits[0].completedCount").value(9))
                .andExpect(jsonPath("$.habits[0].completionRate").value(0.2903))
                .andExpect(jsonPath("$.habits[0].bestStreak").value(5))
                .andExpect(jsonPath("$.overall.scheduledCount").value(31))
                .andExpect(jsonPath("$.overall.completedCount").value(9));
    }

    @Test
    void weekdays_schedule_counts_only_mon_fri() throws Exception {
        MockHttpSession session = userWithHabit("Weekday", "WEEKDAYS", "[]",
                LocalDate.of(2024, 1, 1), "UTC"); // 2024-01-01 is a Monday
        UUID id = firstHabitId(session);

        // Complete the whole first week Mon-Fri.
        for (int d = 1; d <= 5; d++) {
            complete(session, id, "2024-01-0" + d, true);
        }

        mockMvc.perform(get("/api/v1/analytics/habits/" + id
                                + "?from=2024-01-01&to=2024-01-07")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habits[0].scheduledCount").value(5))
                .andExpect(jsonPath("$.habits[0].completedCount").value(5))
                .andExpect(jsonPath("$.habits[0].completionRate").value(1.0))
                .andExpect(jsonPath("$.habits[0].bestStreak").value(5));
    }

    @Test
    void custom_schedule_and_null_rate_when_nothing_scheduled() throws Exception {
        MockHttpSession session = userWithHabit("Custom", "CUSTOM", "[1,3,7]",
                LocalDate.of(2024, 1, 1), "UTC");
        UUID id = firstHabitId(session);

        // Week Jan 1..7: Mon(1), Wed(3), Sun(7) scheduled. Complete Mon + Sun.
        complete(session, id, "2024-01-01", true);
        complete(session, id, "2024-01-07", true);

        mockMvc.perform(get("/api/v1/analytics/habits/" + id
                                + "?from=2024-01-01&to=2024-01-07")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habits[0].scheduledCount").value(3))
                .andExpect(jsonPath("$.habits[0].completedCount").value(2))
                .andExpect(jsonPath("$.habits[0].completionRate").value(0.6667))
                .andExpect(jsonPath("$.habits[0].bestStreak").value(1));

        // Tue-only range: 2024-01-09 is a Tuesday, not in [1,3,7] ->
        // 0 scheduled days -> rate omitted (null, NON_NULL).
        mockMvc.perform(get("/api/v1/analytics/habits/" + id
                                + "?from=2024-01-09&to=2024-01-09")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habits[0].scheduledCount").value(0))
                .andExpect(jsonPath("$.habits[0].completionRate")
                        .doesNotExist()); // null omitted (NON_NULL)
    }

    @Test
    void start_date_clamps_range_and_archived_habits_still_count() throws Exception {
        MockHttpSession session = userWithHabit("Late", "DAILY", "[]",
                LocalDate.of(2024, 1, 15), "UTC");
        UUID id = firstHabitId(session);
        complete(session, id, "2024-01-20", true);

        // Range starts before the habit exists: scheduled only from Jan 15.
        MvcResult result = mockMvc.perform(get(
                        "/api/v1/analytics/summary?from=2024-01-01&to=2024-01-31")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode row = JsonMapper.builder().build()
                .readTree(result.getResponse().getContentAsString())
                .get("habits").get(0);
        assertThat(row.get("scheduledCount").asInt()).isEqualTo(17); // Jan 15..31
        assertThat(row.get("completedCount").asInt()).isEqualTo(1);

        // Archive it; summary still includes its history.
        mockMvc.perform(post("/api/v1/habits/" + id + "/archive")
                        .with(csrf()).session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/analytics/summary?from=2024-01-01&to=2024-01-31")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habits.length()").value(1))
                .andExpect(jsonPath("$.habits[0].completedCount").value(1));
    }

    @Test
    void current_streak_counts_backwards_with_grace_for_today() throws Exception {
        MockHttpSession session = userWithHabit("Streak", "DAILY", "[]",
                LocalDate.now(KOLKATA).minusDays(10), "Asia/Kolkata");
        UUID id = firstHabitId(session);

        LocalDate today = LocalDate.now(KOLKATA);
        // Complete yesterday and the day before; leave today open (grace rule).
        complete(session, id, today.minusDays(1).toString(), true);
        complete(session, id, today.minusDays(2).toString(), true);

        MvcResult result = mockMvc.perform(get(
                        "/api/v1/analytics/summary?from=" + today.minusDays(10)
                                + "&to=" + today)
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode row = JsonMapper.builder().build()
                .readTree(result.getResponse().getContentAsString())
                .get("habits").get(0);
        assertThat(row.get("currentStreak").asInt()).isEqualTo(2);
        assertThat(row.get("bestStreak").asInt()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void invalid_ranges_are_400() throws Exception {
        MockHttpSession session = userWithHabit("Bad", "DAILY", "[]",
                LocalDate.of(2024, 1, 1), "UTC");
        mockMvc.perform(get("/api/v1/analytics/summary?from=2024-02-01&to=2024-01-01")
                        .session(session))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/analytics/summary").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void foreign_habit_analytics_is_404() throws Exception {
        MockHttpSession alice = userWithHabit("Mine", "DAILY", "[]",
                LocalDate.of(2024, 1, 1), "UTC");
        UUID id = firstHabitId(alice);
        MockHttpSession bob = userWithHabit("His", "DAILY", "[]",
                LocalDate.of(2024, 1, 1), "UTC");

        mockMvc.perform(get("/api/v1/analytics/habits/" + id
                        + "?from=2024-01-01&to=2024-01-31")
                        .session(bob))
                .andExpect(status().isNotFound());
    }
}
