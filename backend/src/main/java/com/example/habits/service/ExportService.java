package com.example.habits.service;

import com.example.habits.domain.Habit;
import com.example.habits.domain.HabitCompletion;
import com.example.habits.dto.ExportCompletionDto;
import com.example.habits.dto.ExportJson;
import com.example.habits.dto.HabitDto;
import com.example.habits.dto.SettingsDto;
import com.example.habits.exception.ApiException;
import com.example.habits.repository.HabitCompletionRepository;
import com.example.habits.repository.HabitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GDPR-style data export: JSON document or CSV, strictly scoped to the
 * caller's own rows.
 */
@Service
public class ExportService {

    public enum Format { JSON, CSV }

    private final HabitRepository habitRepository;
    private final HabitCompletionRepository completionRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public ExportService(HabitRepository habitRepository,
                         HabitCompletionRepository completionRepository,
                         UserService userService,
                         ObjectMapper objectMapper) {
        this.habitRepository = habitRepository;
        this.completionRepository = completionRepository;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public Format parseFormat(String raw) {
        if (raw == null || raw.isBlank()) {
            return Format.JSON;
        }
        try {
            return Format.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.validation("format", "must be json or csv");
        }
    }

    @Transactional(readOnly = true)
    public ExportJson exportJson(com.example.habits.domain.User user) {
        List<Habit> habits = habitRepository.findByUserId(user.getId());
        return new ExportJson(
                CurrentUserService.toDto(user),
                userService.getSettings(user),
                habits.stream().map(ExportService::toDto).toList(),
                completionsFor(user.getId(), nameOf(habits)));
    }

    @Transactional(readOnly = true)
    public String renderCsv(com.example.habits.domain.User user) {
        List<Habit> habits = habitRepository.findByUserId(user.getId());
        List<ExportCompletionDto> rows = completionsFor(user.getId(), nameOf(habits));
        StringBuilder out = new StringBuilder();
        out.append("habit_id,habit_name,local_date,completed,completed_at\n");
        for (ExportCompletionDto row : rows) {
            out.append(csvLine(
                    row.habitId().toString(),
                    row.habitName(),
                    row.localDate().toString(),
                    String.valueOf(row.completed()),
                    row.completedAt() == null ? "" : row.completedAt().toString()));
        }
        return out.toString();
    }

    private List<ExportCompletionDto> completionsFor(UUID userId, Map<UUID, String> habitNames) {
        return completionRepository.findAllByUserId(userId).stream()
                .map(c -> new ExportCompletionDto(
                        c.getHabit().getId(),
                        habitNames.getOrDefault(c.getHabit().getId(), ""),
                        c.getLocalDate(),
                        c.isCompleted(),
                        c.getCompletedAt()))
                .sorted(Comparator.comparing(ExportCompletionDto::localDate)
                        .thenComparing(ExportCompletionDto::habitName))
                .toList();
    }

    private static Map<UUID, String> nameOf(List<Habit> habits) {
        Map<UUID, String> names = new HashMap<>();
        for (Habit habit : habits) {
            names.put(habit.getId(), habit.getName());
        }
        return names;
    }

    private static String csvLine(String habitId, String habitName, String localDate,
                                  String completed, String completedAt) {
        return String.join(",",
                habitId,
                csvEscape(habitName),
                localDate,
                completed,
                completedAt) + "\n";
    }

    /** RFC 4180 quoting: wrap fields containing , " CR LF in quotes and double the quotes. */
    private static String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")
                || value.contains("\r")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private static HabitDto toDto(Habit habit) {
        return new HabitDto(habit.getId(), habit.getName(), habit.getDescription(),
                habit.getColor(), habit.getIcon(), habit.getScheduleType(),
                List.copyOf(habit.getScheduleDays()), habit.getStartDate(),
                habit.getArchivedAt(), habit.getCreatedAt(), habit.getUpdatedAt());
    }
}
