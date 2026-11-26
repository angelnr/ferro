package com.ferro.catalog.interfaces.rest;

import java.util.List;

import jakarta.validation.constraints.Size;

public record UpdateExerciseRequest(
        @Size(max = 120) String nameEs,
        @Size(max = 120) String nameEn,
        String muscleGroup,
        List<String> secondaryMuscles,
        String equipment,
        String mechanics,
        Boolean bodyweight,
        Boolean archived) {}
