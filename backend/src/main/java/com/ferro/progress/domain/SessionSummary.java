package com.ferro.progress.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SessionSummary(
        UUID id,
        String name,
        Instant startedAt,
        Instant finishedAt,
        int durationSeconds,
        BigDecimal totalVolume,
        int exerciseCount,
        int setCount,
        boolean hasPr,
        String status) {}
