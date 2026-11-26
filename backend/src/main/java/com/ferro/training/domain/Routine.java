package com.ferro.training.domain;

import java.util.List;
import java.util.UUID;

/** Plantilla de entrenamiento del usuario. */
public record Routine(UUID id, String name, String notes, List<RoutineExercise> exercises) {}
