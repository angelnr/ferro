package com.ferro.progress.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionDetail(
        UUID id,
        String name,
        String notes,
        Instant startedAt,
        Instant finishedAt,
        int durationSeconds,
        BigDecimal totalVolume,
        String status,
        List<ExerciseDetail> exercises) {

    public record ExerciseDetail(
            UUID id,
            UUID exerciseId,
            String name,
            int position,
            BigDecimal volume,
            String notes,
            List<SetDetail> sets,
            List<PrAchieved> personalRecords) {}

    public record SetDetail(
            UUID id,
            int position,
            String setType,
            int reps,
            BigDecimal weight,
            Integer rir,
            BigDecimal volume,
            BigDecimal e1rm,
            String notes,
            Instant loggedAt) {}

    public record PrAchieved(String type, BigDecimal value) {}
}
