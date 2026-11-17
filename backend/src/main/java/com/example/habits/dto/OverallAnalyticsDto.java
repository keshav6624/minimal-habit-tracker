package com.example.habits.dto;

/** Aggregate analytics across all of the user's habits. */
public record OverallAnalyticsDto(Double completionRate, long scheduledCount, long completedCount) {}
