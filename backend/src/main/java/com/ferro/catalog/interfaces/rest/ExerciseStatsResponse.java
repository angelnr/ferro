package com.ferro.catalog.interfaces.rest;

import java.math.BigDecimal;
import java.util.List;

public record ExerciseStatsResponse(
        List<Object> personalRecords,
        BigDecimal bestE1rm,
        BigDecimal totalVolume,
        long sessionCount,
        List<Object> e1rmSeries) {

    public static ExerciseStatsResponse empty() {
        return new ExerciseStatsResponse(List.of(), null, BigDecimal.ZERO, 0, List.of());
    }
}
