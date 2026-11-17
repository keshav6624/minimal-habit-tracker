package com.example.habits.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Completion-rate math: null rate on empty ranges, clamping, exclusions. */
class CompletionRateCalculatorTest {

    @Test
    void empty_range_zero_scheduled_gives_null_rate() {
        var result = CompletionRateCalculator.calculate(0, 0);
        assertEquals(0, result.scheduledCount());
        assertEquals(0, result.completedCount());
        assertNull(result.rate());
    }

    @Test
    void zero_scheduled_with_completions_still_null_rate() {
        var result = CompletionRateCalculator.calculate(0, 5);
        assertNull(result.rate());
        assertEquals(5, result.completedCount());
    }

    @Test
    void partial_week_rate() {
        // 3 of 5 scheduled days completed = 0.6
        var result = CompletionRateCalculator.calculate(5, 3);
        assertEquals(5, result.scheduledCount());
        assertEquals(3, result.completedCount());
        assertEquals(0.6, result.rate());
    }

    @Test
    void full_completion_rate_is_one() {
        var result = CompletionRateCalculator.calculate(20, 20);
        assertEquals(1.0, result.rate());
    }

    @Test
    void rate_is_rounded_to_four_decimals() {
        var result = CompletionRateCalculator.calculate(3, 2);
        assertEquals(0.6667, result.rate());
    }

    @Test
    void negative_completed_count_is_clamped_to_zero() {
        var result = CompletionRateCalculator.calculate(10, -1);
        assertEquals(0, result.completedCount());
        assertEquals(0.0, result.rate());
    }
}
