package com.example.habits.service;

import com.example.habits.domain.ScheduleType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ISO day mapping (1=Mon..7=Sun) for DAILY / WEEKDAYS / CUSTOM schedules. */
class SchedulePolicyTest {

    private static final LocalDate MON = LocalDate.of(2024, 1, 1);
    private static final LocalDate WED = LocalDate.of(2024, 1, 3);
    private static final LocalDate FRI = LocalDate.of(2024, 1, 5);
    private static final LocalDate SAT = LocalDate.of(2024, 1, 6);
    private static final LocalDate SUN = LocalDate.of(2024, 1, 7);

    @Test
    void daily_is_scheduled_every_day_including_weekend() {
        assertTrue(SchedulePolicy.isScheduled(ScheduleType.DAILY, List.of(), MON));
        assertTrue(SchedulePolicy.isScheduled(ScheduleType.DAILY, List.of(), SAT));
        assertTrue(SchedulePolicy.isScheduled(ScheduleType.DAILY, List.of(), SUN));
    }

    @Test
    void weekdays_excludes_saturday_and_sunday() {
        assertTrue(SchedulePolicy.isScheduled(ScheduleType.WEEKDAYS, List.of(), MON));
        assertTrue(SchedulePolicy.isScheduled(ScheduleType.WEEKDAYS, List.of(), FRI));
        assertFalse(SchedulePolicy.isScheduled(ScheduleType.WEEKDAYS, List.of(), SAT));
        assertFalse(SchedulePolicy.isScheduled(ScheduleType.WEEKDAYS, List.of(), SUN));
    }

    @Test
    void custom_matches_iso_day_of_week_sunday_is_7() {
        // 1=Monday .. 7=Sunday
        assertEquals(1, SchedulePolicy.isoDayOfWeek(MON));
        assertEquals(7, SchedulePolicy.isoDayOfWeek(SUN));

        assertTrue(SchedulePolicy.isScheduled(ScheduleType.CUSTOM, List.of(1, 3, 7), MON));
        assertTrue(SchedulePolicy.isScheduled(ScheduleType.CUSTOM, List.of(1, 3, 7), WED));
        assertTrue(SchedulePolicy.isScheduled(ScheduleType.CUSTOM, List.of(1, 3, 7), SUN));
        assertFalse(SchedulePolicy.isScheduled(ScheduleType.CUSTOM, List.of(1, 3, 7), SAT));
        assertFalse(SchedulePolicy.isScheduled(ScheduleType.CUSTOM, List.of(1, 3, 7), FRI));
    }

    @Test
    void custom_with_empty_or_null_list_is_never_scheduled() {
        assertFalse(SchedulePolicy.isScheduled(ScheduleType.CUSTOM, List.of(), MON));
        assertFalse(SchedulePolicy.isScheduled(ScheduleType.CUSTOM, null, MON));
    }

    @Test
    void predicate_matches_isScheduled() {
        var predicate = SchedulePolicy.predicate(ScheduleType.WEEKDAYS, List.of());
        assertTrue(predicate.test(WED));
        assertFalse(predicate.test(SUN));
    }
}
