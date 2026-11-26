package com.ferro.catalog.interfaces.rest;

import java.util.UUID;

import com.ferro.catalog.domain.ExerciseDetail;

public record ExerciseDetailResponse(ExerciseResponse exercise, ExerciseStatsResponse stats) {

    public static ExerciseDetailResponse from(ExerciseDetail detail, UUID userId) {
        return new ExerciseDetailResponse(
                ExerciseResponse.from(detail.exercise(), userId), ExerciseStatsResponse.from(detail.stats()));
    }
}
