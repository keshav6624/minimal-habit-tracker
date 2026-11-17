package com.example.habits.dto;

import java.util.UUID;

/** Per-habit analytics row. */
public record HabitAnalyticsDto(
        UUID habitId,
        String name,
        Integer currentStreak,
        Integer bestStreak,
        Double completionRate,
        long scheduledCount,
        long completedCount) {}
