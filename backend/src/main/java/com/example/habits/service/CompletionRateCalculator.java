package com.example.habits.service;

/**
 * Pure completion-rate logic: rate = completedScheduled / scheduled, or null
 * when nothing is scheduled in the (already clamped) range.
 */
public final class CompletionRateCalculator {

    public record RateResult(long scheduledCount, long completedCount, Double rate) {}

    private CompletionRateCalculator() {
    }

    /**
     * @param rate rounded to 4 decimals, or null when scheduledCount == 0.
     */
    public static RateResult calculate(long scheduledCount, long completedCount) {
        if (scheduledCount <= 0) {
            return new RateResult(0, Math.max(0, completedCount), null);
        }
        double raw = (double) Math.max(0, completedCount) / scheduledCount;
        double rounded = Math.round(raw * 10000.0) / 10000.0;
        return new RateResult(scheduledCount, Math.max(0, completedCount), rounded);
    }
}
