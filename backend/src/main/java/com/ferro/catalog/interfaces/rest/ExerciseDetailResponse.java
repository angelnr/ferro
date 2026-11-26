package com.ferro.catalog.interfaces.rest;

import com.ferro.catalog.domain.Exercise;

public record ExerciseDetailResponse(ExerciseResponse exercise, ExerciseStatsResponse stats) {

    public static ExerciseDetailResponse from(Exercise exercise, java.util.UUID userId) {
        return new ExerciseDetailResponse(ExerciseResponse.from(exercise, userId), ExerciseStatsResponse.empty());
    }
}
