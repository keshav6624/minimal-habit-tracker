package com.example.habits.dto;

import java.time.Instant;
import java.time.LocalDate;

/** One completion record as returned by GET /habits/{id}/completions. */
public record CompletionDto(LocalDate localDate, boolean completed, Instant completedAt) {}
