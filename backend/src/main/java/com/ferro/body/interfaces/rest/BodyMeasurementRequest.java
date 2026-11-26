package com.ferro.body.interfaces.rest;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.Size;

public record BodyMeasurementRequest(
        Instant measuredAt,
        BigDecimal weight,
        BigDecimal chest,
        BigDecimal waist,
        BigDecimal hip,
        BigDecimal arm,
        BigDecimal thigh,
        @Size(max = 500) String notes) {}
