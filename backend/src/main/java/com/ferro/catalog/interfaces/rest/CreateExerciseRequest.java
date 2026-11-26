package com.ferro.catalog.interfaces.rest;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExerciseRequest(
        @Size(max = 120) String nameEs,
        @Size(max = 120) String nameEn,
        @NotBlank String muscleGroup,
        List<String> secondaryMuscles,
        @NotBlank String equipment,
        @NotBlank String mechanics,
        Boolean bodyweight) {}
