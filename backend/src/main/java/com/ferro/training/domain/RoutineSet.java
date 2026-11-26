package com.ferro.training.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Serie objetivo de una plantilla (RN-24). */
public record RoutineSet(
        UUID id, int position, SetType setType, int targetReps, BigDecimal targetWeight, Integer targetRir) {}
