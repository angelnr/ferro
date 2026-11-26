package com.ferro.training.interfaces.rest;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateSetRequest(
        @Min(1) @Max(1000) Integer reps,
        BigDecimal weight,
        @Min(0) @Max(10) Integer rir,
        String setType,
        @Size(max = 500) String notes) {}
