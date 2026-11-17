package com.example.habits.dto;

import java.time.LocalDate;
import java.util.List;

/** Response of GET /habits/{id}/completions?from&to. */
public record CompletionsRangeResponse(LocalDate from, LocalDate to,
                                       List<CompletionDto> completions) {}
