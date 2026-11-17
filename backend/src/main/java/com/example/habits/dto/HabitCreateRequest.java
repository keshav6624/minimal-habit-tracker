package com.example.habits.dto;

import com.example.habits.domain.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Create-habit request. Cross-field schedule rules are enforced in HabitService. */
public record HabitCreateRequest(
        @NotBlank
        @Size(max = 80, message = "must be at most 80 characters")
        String name,

        @Size(max = 500, message = "must be at most 500 characters")
        String description,

        @Size(max = 24, message = "must be at most 24 characters")
        String color,

        @Size(max = 40, message = "must be at most 40 characters")
        String icon,

        @NotNull(message = "is required")
        ScheduleType scheduleType,

        List<Integer> scheduleDays,

        @NotNull(message = "is required (ISO yyyy-MM-dd)")
        LocalDate startDate) {}
