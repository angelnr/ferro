package com.ferro.catalog.interfaces.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.ferro.catalog.domain.ExerciseStats;

public record ExerciseStatsResponse(
        List<PersonalRecordResponse> personalRecords,
        BigDecimal bestE1rm,
        BigDecimal totalVolume,
        long sessionCount,
        List<E1rmPointResponse> e1rmSeries) {

    public static ExerciseStatsResponse from(ExerciseStats stats) {
        return new ExerciseStatsResponse(
                stats.personalRecords().stream()
                        .map(record -> new PersonalRecordResponse(record.type(), record.value(), record.achievedAt()))
                        .toList(),
                stats.bestE1rm(),
                stats.totalVolume(),
                stats.sessionCount(),
                stats.e1rmSeries().stream()
                        .map(point -> new E1rmPointResponse(point.date(), point.value()))
                        .toList());
    }

    public record PersonalRecordResponse(String type, BigDecimal value, Instant achievedAt) {}

    public record E1rmPointResponse(Instant date, BigDecimal value) {}
}
