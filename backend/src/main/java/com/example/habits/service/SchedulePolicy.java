package com.example.habits.service;

import com.example.habits.domain.ScheduleType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Pure, entity-free schedule logic:
 * DAILY   -> every day scheduled
 * WEEKDAYS-> Monday..Friday scheduled
 * CUSTOM  -> scheduled iff ISO day-of-week (1=Mon..7=Sun) is in scheduleDays
 */
public final class SchedulePolicy {

    private SchedulePolicy() {
    }

    /** Is {@code date} a scheduled day for this schedule? */
    public static boolean isScheduled(ScheduleType type, List<Integer> scheduleDays, LocalDate date) {
        return switch (type) {
            case DAILY -> true;
            case WEEKDAYS -> date.getDayOfWeek() != DayOfWeek.SATURDAY
                          && date.getDayOfWeek() != DayOfWeek.SUNDAY;
            case CUSTOM -> scheduleDays != null
                    && scheduleDays.contains(isoDayOfWeek(date));
        };
    }

    /** Predicate form for streak/rate calculators. */
    public static Predicate<LocalDate> predicate(ScheduleType type, List<Integer> scheduleDays) {
        return date -> isScheduled(type, scheduleDays, date);
    }

    /** ISO day-of-week: Monday=1 .. Sunday=7 (matches DayOfWeek.getValue()). */
    public static int isoDayOfWeek(LocalDate date) {
        return date.getDayOfWeek().getValue();
    }

    /** Reusable validator for the 1..7 subset rule. */
    public static final BiPredicate<List<Integer>, Integer> VALID_CUSTOM_DAYS =
            (days, size) -> days != null && size > 0
                    && days.stream().allMatch(d -> d >= 1 && d <= 7);
}
