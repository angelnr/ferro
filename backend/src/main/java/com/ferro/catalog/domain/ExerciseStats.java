package com.ferro.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ExerciseStats(
        List<PersonalRecord> personalRecords,
        BigDecimal bestE1rm,
        BigDecimal totalVolume,
        long sessionCount,
        List<E1rmPoint> e1rmSeries) {

    public static ExerciseStats empty() {
        return new ExerciseStats(List.of(), null, BigDecimal.ZERO, 0, List.of());
    }

    public record PersonalRecord(String type, BigDecimal value, Instant achievedAt) {}

    public record E1rmPoint(Instant date, BigDecimal value) {}
}
