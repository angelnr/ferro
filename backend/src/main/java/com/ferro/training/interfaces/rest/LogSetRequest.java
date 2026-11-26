package com.ferro.training.interfaces.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LogSetRequest(
        @NotNull @Min(1) @Max(1000) Integer reps,
        @NotNull java.math.BigDecimal weight,
        @Min(0) @Max(10) Integer rir,
        String setType,
        String notes,
        java.util.UUID targetSetId) {}
