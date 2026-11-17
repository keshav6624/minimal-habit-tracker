package com.example.habits.dto;

import com.example.habits.domain.ScheduleType;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Partial update: every field is nullable — null means "leave unchanged".
 * An explicit empty description string clears it; a blank name is rejected.
 */
public record HabitUpdateRequest(
        @Size(max = 80, message = "must be at most 80 characters")
        String name,

        @Size(max = 500, message = "must be at most 500 characters")
        String description,

        @Size(max = 24, message = "must be at most 24 characters")
        String color,

        @Size(max = 40, message = "must be at most 40 characters")
        String icon,

        ScheduleType scheduleType,

        List<Integer> scheduleDays,

        LocalDate startDate) {}
