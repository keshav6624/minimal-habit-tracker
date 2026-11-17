package com.example.habits.service;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Pure streak logic over completed local dates, a schedule predicate, the
 * habit's start date, and "today".
 *
 * <p>Definitions (see API_SPEC / PRD):
 * <ul>
 *   <li><b>bestStreak</b>: longest run of completed scheduled days from
 *       start date through today.</li>
 *   <li><b>currentStreak</b>: consecutive completed scheduled days ending at
 *       today (or the last scheduled past day). Grace rule: if today is
 *       scheduled but not yet completed, today does NOT break the streak —
 *       counting resumes from the previous scheduled day.</li>
 * </ul></p>
 */
public final class StreakCalculator {

    public record StreakResult(int currentStreak, int bestStreak) {}

    private StreakCalculator() {
    }

    public static StreakResult calculate(Predicate<LocalDate> scheduled,
                                         LocalDate startDate,
                                         LocalDate today,
                                         Set<LocalDate> completedDates) {

        int best = 0;
        int run = 0;
        for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
            if (scheduled.test(d)) {
                if (completedDates.contains(d)) {
                    run++;
                    best = Math.max(best, run);
                } else {
                    run = 0;
                }
            }
        }

        // Current streak: walk backwards; skip an incomplete today (grace rule).
        int current = 0;
        LocalDate cursor = today;
        if (scheduled.test(today) && !completedDates.contains(today)) {
            cursor = today.minusDays(1);
        }
        while (!cursor.isBefore(startDate)) {
            if (scheduled.test(cursor)) {
                if (completedDates.contains(cursor)) {
                    current++;
                } else {
                    break; // A scheduled-but-incomplete past day ends the run.
                }
            }
            cursor = cursor.minusDays(1);
        }

        return new StreakResult(current, best);
    }
}
