package com.ferro.training.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NewPersonalRecord(
        UUID exerciseId,
        String exerciseName,
        PrType prType,
        BigDecimal value,
        BigDecimal previousValue,
        Instant achievedAt) {}
