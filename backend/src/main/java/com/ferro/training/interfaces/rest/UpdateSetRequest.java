package com.ferro.training.interfaces.rest;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateSetRequest(
        @Min(1) @Max(1000) Integer reps,
        BigDecimal weight,
        @Min(0) @Max(10) Integer rir,
        String setType) {}
