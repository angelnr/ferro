package com.ferro.body.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BodyMeasurement(
        UUID id,
        Instant measuredAt,
        BigDecimal weight,
        BigDecimal chest,
        BigDecimal waist,
        BigDecimal hip,
        BigDecimal arm,
        BigDecimal thigh,
        String notes) {}
