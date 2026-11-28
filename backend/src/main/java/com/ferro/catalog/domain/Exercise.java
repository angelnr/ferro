package com.ferro.catalog.domain;

import java.util.List;
import java.util.UUID;

/** Ejercicio del catálogo global o custom del usuario (M2). */
public record Exercise(
        UUID id,
        String nameEs,
        String nameEn,
        MuscleGroup muscleGroup,
        List<MuscleGroup> secondaryMuscles,
        Equipment equipment,
        Mechanics mechanics,
        boolean bodyweight,
        boolean custom,
        UUID ownerId,
        boolean archived,
        String imageUrl) {}
