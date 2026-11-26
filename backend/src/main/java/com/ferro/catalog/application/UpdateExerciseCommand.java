package com.ferro.catalog.application;

import java.util.List;

public record UpdateExerciseCommand(
        String nameEs,
        String nameEn,
        String muscleGroup,
        List<String> secondaryMuscles,
        String equipment,
        String mechanics,
        Boolean bodyweight,
        Boolean archived) {}
