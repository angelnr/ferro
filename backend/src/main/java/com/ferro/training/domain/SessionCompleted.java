package com.ferro.training.domain;

import java.time.Instant;
import java.util.UUID;

public record SessionCompleted(UUID sessionId, UUID userId, Instant finishedAt, int newPersonalRecords) {}
