package com.ferro.training.interfaces.rest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoutineRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 500) String notes,
        @NotNull @Size(min = 1, max = 30) List<@Valid ExerciseRequest> exercises) {

    public record ExerciseRequest(
            @NotNull UUID exerciseId,
            @Min(15) @Max(600) Integer restSeconds,
            Integer supersetGroup,
            @Size(max = 500) String notes,
            @NotNull @Size(max = 15) List<@Valid SetRequest> sets) {}

    public record SetRequest(
            String setType,
            @NotNull @Min(1) @Max(1000) Integer targetReps,
            BigDecimal targetWeight,
            @Min(0) @Max(10) Integer targetRir) {}
}
