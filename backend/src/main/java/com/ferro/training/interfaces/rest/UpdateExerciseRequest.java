package com.ferro.training.interfaces.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateExerciseRequest(
        Integer position,
        @Min(15) @Max(600) Integer restSeconds,
        Integer supersetGroup,
        @Size(max = 500) String notes) {}
