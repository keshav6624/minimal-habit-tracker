package com.example.habits.domain;

/**
 * Schedule recurrence policy for a habit.
 */
public enum ScheduleType {
    /** Every day of the week. */
    DAILY,
    /** Monday through Friday. */
    WEEKDAYS,
    /** Specific ISO day-of-week ints 1..7 (1=Mon .. 7=Sun) stored in scheduleDays. */
    CUSTOM
}
