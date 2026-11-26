package com.ferro.training.domain;

import java.time.Instant;
import java.util.UUID;

public record RoutineSummary(UUID id, String name, int exerciseCount, Instant lastUsedAt) {}
