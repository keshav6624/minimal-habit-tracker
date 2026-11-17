package com.example.habits.service;

import com.example.habits.domain.ScheduleType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Streak math incl. leap years (Feb 2024), month/year boundaries, custom
 * schedules with gaps, the "today incomplete" grace rule, best streak, and
 * empty history.
 */
class StreakCalculatorTest {

    private static final Predicate<LocalDate> EVERY_DAY = d -> true;
    private static final Predicate<LocalDate> NEVER = d -> false;

    @Test
    void empty_history_gives_zero_streaks() {
        var result = StreakCalculator.calculate(EVERY_DAY,
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 28), Set.of());
        assertEquals(0, result.currentStreak());
        assertEquals(0, result.bestStreak());
    }

    @Test
    void leap_year_february_2024_has_29_days() {
        LocalDate start = LocalDate.of(2024, 2, 1);
        LocalDate end = LocalDate.of(2024, 2, 29);
        Set<LocalDate> completed = Set.of(
                LocalDate.of(2024, 2, 27),
                LocalDate.of(2024, 2, 28),
                LocalDate.of(2024, 2, 29));
        var result = StreakCalculator.calculate(EVERY_DAY, start, end, completed);
        assertEquals(3, result.currentStreak()); // ends on today
        assertEquals(3, result.bestStreak());
    }

    @Test
    void month_and_year_boundary_counted_correctly() {
        Set<LocalDate> completed = Set.of(
                LocalDate.of(2023, 12, 30),
                LocalDate.of(2023, 12, 31),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 2));
        var result = StreakCalculator.calculate(EVERY_DAY,
                LocalDate.of(2023, 12, 30), LocalDate.of(2024, 1, 2), completed);
        assertEquals(4, result.currentStreak());
        assertEquals(4, result.bestStreak());
    }

    @Test
    void gap_resets_current_and_best_is_longest_run() {
        // Run A: Jan 1-3; run B: Jan 6-9 (longer). Today = Jan 9.
        Set<LocalDate> completed = Set.of(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2),
                LocalDate.of(2024, 1, 3),
                LocalDate.of(2024, 1, 6), LocalDate.of(2024, 1, 7),
                LocalDate.of(2024, 1, 8), LocalDate.of(2024, 1, 9));
        var result = StreakCalculator.calculate(EVERY_DAY,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 9), completed);
        assertEquals(4, result.currentStreak());
        assertEquals(4, result.bestStreak());
    }

    @Test
    void custom_schedule_gaps_do_not_break_streak() {
        // Scheduled Mon/Wed/Fri (ISO 1,3,5); completed all three of the last week.
        var schedule = SchedulePolicy.predicate(ScheduleType.CUSTOM, List.of(1, 3, 5));
        Set<LocalDate> completed = Set.of(
                LocalDate.of(2024, 1, 1), // Mon
                LocalDate.of(2024, 1, 3), // Wed
                LocalDate.of(2024, 1, 5)); // Fri
        var result = StreakCalculator.calculate(schedule,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), completed);
        // Three consecutive SCHEDULED days completed -> streak 3.
        assertEquals(3, result.currentStreak());
        assertEquals(3, result.bestStreak());
    }

    @Test
    void custom_schedule_missed_scheduled_day_breaks_streak() {
        var schedule = SchedulePolicy.predicate(ScheduleType.CUSTOM, List.of(1, 3, 5));
        // Wed completed, Fri scheduled but NOT completed, then Mon completed.
        Set<LocalDate> completed = Set.of(
                LocalDate.of(2024, 1, 3),
                LocalDate.of(2024, 1, 8));
        var result = StreakCalculator.calculate(schedule,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 8), completed);
        assertEquals(1, result.currentStreak()); // only Mon Jan 8
        assertEquals(1, result.bestStreak());
    }

    @Test
    void grace_rule_today_scheduled_but_incomplete_does_not_break() {
        // Completed through yesterday; today (Jan 5) scheduled but not yet done.
        Set<LocalDate> completed = Set.of(
                LocalDate.of(2024, 1, 2),
                LocalDate.of(2024, 1, 3),
                LocalDate.of(2024, 1, 4));
        var result = StreakCalculator.calculate(EVERY_DAY,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5), completed);
        assertEquals(3, result.currentStreak()); // counts Jan 4 backward, grace on Jan 5
        assertEquals(3, result.bestStreak());
    }

    @Test
    void grace_rule_counts_backward_from_previous_scheduled_day() {
        // Custom Mon(1)/Wed(3): today = Wed Jan 3, not yet completed; Mon completed.
        var schedule = SchedulePolicy.predicate(ScheduleType.CUSTOM, List.of(1, 3));
        Set<LocalDate> completed = Set.of(LocalDate.of(2024, 1, 1));
        var result = StreakCalculator.calculate(schedule,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3), completed);
        assertEquals(1, result.currentStreak());
        assertEquals(1, result.bestStreak());
    }

    @Test
    void missed_scheduled_day_in_the_past_breaks_streak_despite_today() {
        // Yesterday (scheduled) incomplete -> no grace for past days.
        Set<LocalDate> completed = Set.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2));
        var result = StreakCalculator.calculate(EVERY_DAY,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 4), completed);
        assertEquals(0, result.currentStreak()); // Jan 3 incomplete breaks the run
        assertEquals(2, result.bestStreak());
    }

    @Test
    void never_scheduled_dates_are_ignored() {
        var result = StreakCalculator.calculate(NEVER,
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 10),
                Set.of(LocalDate.of(2024, 2, 3)));
        assertEquals(0, result.currentStreak());
        assertEquals(0, result.bestStreak());
    }

    @Test
    void start_date_clamping_excludes_days_before_start() {
        // Completed Jan 1 and Jan 15, but habit only starts Jan 10.
        Set<LocalDate> completed = Set.of(LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 15));
        var result = StreakCalculator.calculate(EVERY_DAY,
                LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 15), completed);
        assertEquals(1, result.currentStreak());
        assertEquals(1, result.bestStreak());
    }
}
