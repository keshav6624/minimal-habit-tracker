package com.example.habits.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** JSON export payload for GET /api/v1/users/me/export?format=json. */
public record ExportJson(UserDto user,
                         SettingsDto settings,
                         List<HabitDto> habits,
                         List<ExportCompletionDto> completions) {}
