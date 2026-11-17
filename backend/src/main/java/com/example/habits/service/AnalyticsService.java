package com.example.habits.service;

import com.example.habits.domain.Habit;
import com.example.habits.domain.User;
import com.example.habits.dto.AnalyticsSummaryDto;
import com.example.habits.dto.HabitAnalyticsDto;
import com.example.habits.dto.OverallAnalyticsDto;
import com.example.habits.exception.ApiException;
import com.example.habits.exception.NotFoundException;
import com.example.habits.repository.HabitCompletionRepository;
import com.example.habits.repository.HabitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Schedule-aware analytics. Pure math lives in {@link SchedulePolicy},
 * {@link StreakCalculator} and {@link CompletionRateCalculator}; this class
 * only clamps ranges, aggregates rows, and assembles DTOs.
 */
@Service
public class AnalyticsService {

    private final HabitRepository habitRepository;
    private final HabitCompletionRepository completionRepository;
    private final TimezoneService timezoneService;

    public AnalyticsService(HabitRepository habitRepository,
                            HabitCompletionRepository completionRepository,
                            TimezoneService timezoneService) {
        this.habitRepository = habitRepository;
        this.completionRepository = completionRepository;
        this.timezoneService = timezoneService;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryDto summary(User user, LocalDate from, LocalDate to) {
        requireRange(from, to);
        List<Habit> habits = habitRepository.findByUserId(user.getId()); // archived included
        return compute(user, from, to, habits, null);
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryDto singleHabit(User user, UUID habitId, LocalDate from,
                                           LocalDate to) {
        requireRange(from, to);
        Habit habit = habitRepository.findByIdAndUserId(habitId, user.getId())
                .orElseThrow(() -> new NotFoundException("Habit not found"));
        return compute(user, from, to, List.of(habit), habitId);
    }

    private void requireRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw ApiException.validation("from", "from and to are both required (YYYY-MM-DD)");
        }
        if (from.isAfter(to)) {
            throw ApiException.validation("from", "from must be on or before to");
        }
    }

    private AnalyticsSummaryDto compute(User user, LocalDate from, LocalDate to,
                                        List<Habit> habits, UUID onlyHabitId) {
        ZoneId zone = timezoneService.validate(user.getTimezone());
        LocalDate today = timezoneService.today(zone);

        // One join query over the user's completions, grouped per habit in memory.
        Map<UUID, Set<LocalDate>> completionsByHabit = new HashMap<>();
        for (var completion : completionRepository.findAllByUserIdAndDateRange(
                user.getId(), from, to)) {
            if (completion.isCompleted()) {
                completionsByHabit.computeIfAbsent(completion.getHabit().getId(),
                        id -> new HashSet<>()).add(completion.getLocalDate());
            }
        }

        List<HabitAnalyticsDto> rows = new ArrayList<>();
        long overallScheduled = 0;
        long overallCompleted = 0;

        for (Habit habit : habits) {
            // Clamp: [max(startDate, from), min(to, today)]; future dates excluded.
            LocalDate rangeStart = habit.getStartDate().isAfter(from)
                    ? habit.getStartDate() : from;
            LocalDate rangeEnd = to.isAfter(today) ? today : to;

            var schedule = SchedulePolicy.predicate(habit.getScheduleType(),
                    habit.getScheduleDays());
            // Restrict to the clamped range (the query used the raw [from, to]).
            Set<LocalDate> completed = completionsByHabit
                    .getOrDefault(habit.getId(), Set.of()).stream()
                    .filter(d -> !d.isBefore(rangeStart) && !d.isAfter(rangeEnd))
                    .collect(Collectors.toSet());

            long scheduledCount = 0;
            long completedCount = 0;
            for (LocalDate d = rangeStart; !d.isAfter(rangeEnd); d = d.plusDays(1)) {
                if (schedule.test(d)) {
                    scheduledCount++;
                    if (completed.contains(d)) {
                        completedCount++;
                    }
                }
            }
            overallScheduled += scheduledCount;
            overallCompleted += completedCount;

            var rate = CompletionRateCalculator.calculate(scheduledCount, completedCount);
            var streaks = StreakCalculator.calculate(schedule, habit.getStartDate(), today,
                    new HashSet<>(completed));

            rows.add(new HabitAnalyticsDto(habit.getId(), habit.getName(),
                    streaks.currentStreak(), streaks.bestStreak(), rate.rate(),
                    rate.scheduledCount(), rate.completedCount()));
        }

        var overallRate = CompletionRateCalculator.calculate(overallScheduled, overallCompleted);
        OverallAnalyticsDto overall = new OverallAnalyticsDto(overallRate.rate(),
                overallRate.scheduledCount(), overallRate.completedCount());

        List<HabitAnalyticsDto> filtered = onlyHabitId == null
                ? rows
                : rows.stream().filter(r -> r.habitId().equals(onlyHabitId)).toList();
        return new AnalyticsSummaryDto(from, to, filtered, overall);
    }
}
