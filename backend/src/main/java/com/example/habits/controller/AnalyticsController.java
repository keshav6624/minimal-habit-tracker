package com.example.habits.controller;

import com.example.habits.dto.AnalyticsSummaryDto;
import com.example.habits.service.AnalyticsService;
import com.example.habits.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Schedule-aware analytics (streaks, completion rates).
 */
@Tag(name = "analytics")
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUserService;

    public AnalyticsController(AnalyticsService analyticsService,
                               CurrentUserService currentUserService) {
        this.analyticsService = analyticsService;
        this.currentUserService = currentUserService;
    }

    @Operation(summary = "Summary across all habits (archived history included)")
    @GetMapping("/summary")
    public AnalyticsSummaryDto summary(@RequestParam LocalDate from,
                                       @RequestParam LocalDate to) {
        return analyticsService.summary(currentUserService.requireUser(), from, to);
    }

    @Operation(summary = "Single-habit summary")
    @GetMapping("/habits/{habitId}")
    public AnalyticsSummaryDto habit(@PathVariable UUID habitId,
                                     @RequestParam LocalDate from,
                                     @RequestParam LocalDate to) {
        return analyticsService.singleHabit(currentUserService.requireUser(),
                habitId, from, to);
    }
}
