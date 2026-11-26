package com.ferro.training.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Serie objetivo copiada a una sesión activa desde una rutina (RN-24). */
public record TargetSet(
        UUID id, int position, SetType setType, int targetReps, BigDecimal targetWeight, Integer targetRir) {}
