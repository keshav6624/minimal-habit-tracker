package com.example.habits.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** User settings (GET response and PUT request). */
public record SettingsDto(
        @NotBlank(message = "must not be blank (IANA timezone)")
        String timezone,

        @NotNull(message = "is required (1=Mon .. 7=Sun)")
        @Min(value = 1, message = "must be between 1 and 7")
        @Max(value = 7, message = "must be between 1 and 7")
        Integer weekStartsOn,

        @NotNull(message = "is required")
        Boolean remindersEnabled) {}
