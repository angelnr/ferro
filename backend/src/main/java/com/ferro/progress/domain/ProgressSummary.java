package com.ferro.progress.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProgressSummary(
        int weeks,
        List<WeekPoint> weeklyVolume,
        List<WeekPoint> weeklySessions,
        int currentStreak,
        int bestStreak,
        PeriodTotals current,
        PeriodTotals previous,
        List<MuscleSlice> muscleDistribution,
        Instant periodStart,
        Instant periodEnd) {

    public record WeekPoint(Instant weekStart, BigDecimal value) {}

    public record PeriodTotals(BigDecimal volume, long sessions, long sets) {}

    public record MuscleSlice(String muscleGroup, BigDecimal volume) {}
}
