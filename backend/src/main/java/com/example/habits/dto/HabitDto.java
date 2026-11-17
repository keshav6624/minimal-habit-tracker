package com.example.habits.dto;

import com.example.habits.domain.ScheduleType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Habit representation returned by the API. */
public record HabitDto(
        java.util.UUID id,
        String name,
        String description,
        String color,
        String icon,
        ScheduleType scheduleType,
        List<Integer> scheduleDays,
        LocalDate startDate,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt) {}
