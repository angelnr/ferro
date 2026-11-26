package com.ferro.training.domain;

import java.util.List;
import java.util.UUID;

public record RoutineExercise(
        UUID id,
        UUID exerciseId,
        String exerciseName,
        int position,
        Integer restSeconds,
        Integer supersetGroup,
        String notes,
        List<RoutineSet> sets) {}
