package com.example.habits.dto;

import java.time.LocalDate;
import java.util.List;

/** Response of GET /api/v1/analytics/summary (and the single-habit variant). */
public record AnalyticsSummaryDto(LocalDate from, LocalDate to,
                                  List<HabitAnalyticsDto> habits,
                                  OverallAnalyticsDto overall) {}
