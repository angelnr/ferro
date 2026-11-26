package com.ferro.training.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SessionSet(
        UUID id,
        int position,
        SetType setType,
        int reps,
        BigDecimal weight,
        Integer rir,
        BigDecimal volume,
        BigDecimal e1rm,
        Instant loggedAt) {}
