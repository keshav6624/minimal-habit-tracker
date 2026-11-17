package com.example.habits.controller;

import com.example.habits.dto.UserDto;
import com.example.habits.exception.ErrorType;
import com.example.habits.exception.NotFoundException;
import com.example.habits.repository.UserRepository;
import com.example.habits.security.SecurityConfig;
import com.example.habits.service.CurrentUserService;
import com.example.habits.service.ExportService;
import com.example.habits.service.HabitService;
import com.example.habits.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Habit slice: validation statuses, 401 unauthenticated, 404 cross-tenant. */
@WebMvcTest({HabitsController.class, UsersController.class})
@Import(SecurityConfig.class)
class HabitsWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HabitService habitService;

    @MockBean
    private UserService userService;

    @MockBean
    private ExportService exportService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private UserRepository userRepository;

    private final UUID habitId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private void stubAuthenticatedUser() {
        UserDto dto = new UserDto(userId, "a@b.com", "A", "Asia/Kolkata");
        when(currentUserService.requireUser()).thenReturn(
                Mockito.mock(com.example.habits.domain.User.class));
        Mockito.lenient().when(userService.getMe(any())).thenReturn(dto);
    }

    @Test
    void unauthenticated_get_habits_is_401_with_envelope() throws Exception {
        mockMvc.perform(get("/api/v1/habits"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ErrorType.UNAUTHORIZED.name()))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void unauthenticated_get_me_is_401_with_envelope() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ErrorType.UNAUTHORIZED.name()));
    }

    @Test
    @WithMockUser(username = "a@b.com")
    void create_habit_blank_name_is_400_with_field_error() throws Exception {
        stubAuthenticatedUser();
        mockMvc.perform(post("/api/v1/habits")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","scheduleType":"DAILY","startDate":"2024-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorType.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    @WithMockUser(username = "a@b.com")
    void create_habit_missing_schedule_type_is_400() throws Exception {
        stubAuthenticatedUser();
        mockMvc.perform(post("/api/v1/habits")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Run","startDate":"2024-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("scheduleType"));
    }

    @Test
    @WithMockUser(username = "a@b.com")
    void malformed_uuid_path_returns_400() throws Exception {
        stubAuthenticatedUser();
        when(habitService.get(any(), eq(habitId)))
                .thenThrow(new IllegalStateException("should not be reached"));
        mockMvc.perform(get("/api/v1/habits/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ErrorType.VALIDATION_ERROR.name()));
    }

    @Test
    @WithMockUser(username = "a@b.com")
    void foreign_habit_returns_404_not_403() throws Exception {
        stubAuthenticatedUser();
        when(habitService.get(any(), eq(habitId)))
                .thenThrow(new NotFoundException("Habit not found"));
        mockMvc.perform(get("/api/v1/habits/" + habitId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ErrorType.NOT_FOUND.name()));
    }

    @Test
    @WithMockUser(username = "a@b.com")
    void oversized_name_is_400() throws Exception {
        stubAuthenticatedUser();
        String longName = "x".repeat(81);
        mockMvc.perform(post("/api/v1/habits")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + longName
                                + "\",\"scheduleType\":\"DAILY\",\"startDate\":\"2024-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }
}
