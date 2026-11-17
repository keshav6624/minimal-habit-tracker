package com.example.habits.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One row of the JSON/CSV export. */
public record ExportCompletionDto(UUID habitId,
                                  String habitName,
                                  LocalDate localDate,
                                  boolean completed,
                                  Instant completedAt) {}
