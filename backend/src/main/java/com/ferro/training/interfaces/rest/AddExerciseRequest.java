package com.ferro.training.interfaces.rest;

import jakarta.validation.constraints.NotNull;

public record AddExerciseRequest(@NotNull java.util.UUID exerciseId) {}
