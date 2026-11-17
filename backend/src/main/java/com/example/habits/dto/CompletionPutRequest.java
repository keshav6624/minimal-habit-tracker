package com.example.habits.dto;

import java.time.LocalDate;
import java.time.Instant;

/** Body of PUT /habits/{id}/completions/{localDate}; absent flag defaults to true. */
public record CompletionPutRequest(Boolean completed) {}
