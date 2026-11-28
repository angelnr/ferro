package com.ferro.catalog.interfaces.rest;

import java.util.List;
import java.util.UUID;

import com.ferro.catalog.domain.Exercise;

public record ExerciseResponse(
        UUID id,
        String name,
        String nameEn,
        String muscleGroup,
        List<String> secondaryMuscles,
        String equipment,
        String mechanics,
        boolean bodyweight,
        boolean mine,
        String imageUrl) {

    public static ExerciseResponse from(Exercise exercise, UUID userId) {
        return new ExerciseResponse(
                exercise.id(),
                exercise.nameEs(),
                exercise.nameEn(),
                exercise.muscleGroup().name(),
                exercise.secondaryMuscles().stream().map(Enum::name).toList(),
                exercise.equipment().name(),
                exercise.mechanics().name(),
                exercise.bodyweight(),
                exercise.ownerId() != null && exercise.ownerId().equals(userId),
                exercise.imageUrl());
    }
}
