package com.example.habits.integration;

import com.example.habits.domain.Habit;
import com.example.habits.domain.User;
import com.example.habits.domain.UserSettings;
import com.example.habits.exception.ErrorType;
import com.example.habits.repository.HabitCompletionRepository;
import com.example.habits.repository.UserRepository;
import com.example.habits.repository.UserSettingsRepository;
import com.example.habits.service.CompletionService;
import com.example.habits.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Completion upsert/delete/range rules + the concurrent-duplicate guarantee. */
class CompletionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private com.example.habits.repository.HabitRepository habitRepository;

    @Autowired
    private HabitCompletionRepository completionRepository;

    @Autowired
    private CompletionService completionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    /** One transaction so the user stays managed while settings/habit attach to it. */
    private record RaceSetup(UUID userId, UUID habitId) {}

    private RaceSetup newRaceSetup() {
        var template = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        return template.execute(status -> {
            User user = new User();
            user.setEmail(uniqueEmail("race"));
            user.setPasswordHash(passwordEncoder.encode(STRONG_PASSWORD));
            user.setTimezone("Asia/Kolkata");
            userRepository.saveAndFlush(user);
            userSettingsRepository.save(new UserSettings(user));
            Habit habit = new Habit();
            habit.setUser(user);
            habit.setName("Race habit");
            habit.setScheduleType(com.example.habits.domain.ScheduleType.DAILY);
            habit.setStartDate(LocalDate.of(2024, 1, 1));
            habitRepository.saveAndFlush(habit);
            return new RaceSetup(user.getId(), habit.getId());
        });
    }

    private MockHttpSession userWithHabit(String suffix, LocalDate start)
            throws Exception {
        MockHttpSession session = register(uniqueEmail("comp" + suffix),
                STRONG_PASSWORD, "C", "Asia/Kolkata");
        mockMvc.perform(post("/api/v1/habits")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Run %s","scheduleType":"DAILY","startDate":"%s"}
                                """.formatted(suffix, start)))
                .andExpect(status().isCreated());
        return session;
    }

    private UUID firstHabitId(MockHttpSession session) throws Exception {
        MvcResult list = mockMvc.perform(get("/api/v1/habits").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(list.getResponse().getContentAsString())
                .get("content").get(0).get("id").asText());
    }

    @Test
    void put_is_idempotent_upsert_with_flag_semantics() throws Exception {
        MockHttpSession session = userWithHabit("1", LocalDate.of(2024, 1, 1));
        UUID habitId = firstHabitId(session);
        LocalDate date = LocalDate.of(2024, 3, 10);

        // Default body (absent flag) => completed true.
        mockMvc.perform(put("/api/v1/habits/" + habitId + "/completions/" + date)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        // Re-PUT same value: still one row, updated timestamps.
        mockMvc.perform(put("/api/v1/habits/" + habitId + "/completions/" + date)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        // Explicit false: completed_at cleared.
        mockMvc.perform(put("/api/v1/habits/" + habitId + "/completions/" + date)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.completedAt").doesNotExist());

        assertThat(completionRepository
                .findByHabitIdAndLocalDateBetweenOrderByLocalDateAsc(habitId, date, date))
                .hasSize(1);
    }

    @Test
    void future_date_and_before_start_are_400() throws Exception {
        MockHttpSession session = userWithHabit("2", LocalDate.of(2024, 1, 1));
        UUID habitId = firstHabitId(session);
        LocalDate tomorrow = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).plusDays(1);

        mockMvc.perform(put("/api/v1/habits/" + habitId + "/completions/" + tomorrow)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorType.VALIDATION_ERROR.name()));

        mockMvc.perform(put("/api/v1/habits/" + habitId + "/completions/2023-12-31")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorType.VALIDATION_ERROR.name()));
    }

    @Test
    void archived_habit_put_is_409_then_restore_makes_it_work() throws Exception {
        MockHttpSession session = userWithHabit("3", LocalDate.of(2024, 1, 1));
        UUID habitId = firstHabitId(session);
        String url = "/api/v1/habits/" + habitId + "/completions/2024-02-01";

        mockMvc.perform(post("/api/v1/habits/" + habitId + "/archive")
                        .with(csrf()).session(session))
                .andExpect(status().isOk());

        mockMvc.perform(put(url).with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ErrorType.CONFLICT.name()));

        mockMvc.perform(post("/api/v1/habits/" + habitId + "/restore")
                        .with(csrf()).session(session))
                .andExpect(status().isOk());

        mockMvc.perform(put(url).with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void delete_removes_record_404_when_absent() throws Exception {
        MockHttpSession session = userWithHabit("4", LocalDate.of(2024, 1, 1));
        UUID habitId = firstHabitId(session);
        String url = "/api/v1/habits/" + habitId + "/completions/2024-02-02";

        mockMvc.perform(put(url).with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete(url).with(csrf()).session(session))
                .andExpect(status().isNoContent());

        // Second delete: nothing to remove -> 404.
        mockMvc.perform(delete(url).with(csrf()).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void range_rules_from_le_to_and_max_370_days() throws Exception {
        MockHttpSession session = userWithHabit("5", LocalDate.of(2024, 1, 1));
        UUID habitId = firstHabitId(session);
        String base = "/api/v1/habits/" + habitId + "/completions";

        mockMvc.perform(put(base + "/2024-01-05").with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(put(base + "/2024-01-06").with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(base + "?from=2024-01-01&to=2024-01-10").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2024-01-01"))
                .andExpect(jsonPath("$.to").value("2024-01-10"))
                .andExpect(jsonPath("$.completions.length()").value(2))
                .andExpect(jsonPath("$.completions[0].localDate").value("2024-01-05"))
                .andExpect(jsonPath("$.completions[0].completed").value(true));

        mockMvc.perform(get(base + "?from=2024-01-10&to=2024-01-01").session(session))
                .andExpect(status().isBadRequest());

        // 371 days -> 400.
        mockMvc.perform(get(base + "?from=2024-01-01&to=2025-01-06").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrent_duplicate_puts_yield_exactly_one_row_and_both_succeed()
            throws Exception {
        // Service-level concurrency: two threads race the same (habit, date).
        // Uniqueness backstop: (habit_id, local_date); the loser catches
        // DataIntegrityViolationException and re-selects.
        RaceSetup setup = newRaceSetup();
        UUID habitId = setup.habitId();
        LocalDate date = LocalDate.of(2024, 5, 5);

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Object>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await(5, TimeUnit.SECONDS);
                // Each "request" resolves its own caller (detached after the
                // repository transaction ends; the service only reads the id).
                User owner = userRepository.findById(setup.userId()).orElseThrow();
                completionService.upsert(owner, habitId, date, true);
                return Boolean.TRUE;
            }));
        }
        ready.await(5, TimeUnit.SECONDS);
        go.countDown();
        for (Future<Object> future : futures) {
            future.get(30, TimeUnit.SECONDS); // both/all requests succeeded
        }
        pool.shutdown();

        long rows = completionRepository.findAll().stream()
                .filter(c -> c.getHabit().getId().equals(habitId)
                        && c.getLocalDate().equals(date))
                .count();
        assertThat(rows).isEqualTo(1); // exactly one row
    }
}
