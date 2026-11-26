package com.ferro.training.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkoutSession(
        UUID id,
        String name,
        String notes,
        SessionStatus status,
        Instant startedAt,
        Instant finishedAt,
        Integer durationSeconds,
        BigDecimal totalVolume,
        List<SessionExercise> exercises) {}
