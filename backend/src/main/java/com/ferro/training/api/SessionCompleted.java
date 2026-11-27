package com.ferro.training.api;

import java.time.Instant;
import java.util.UUID;

public record SessionCompleted(UUID sessionId, UUID userId, Instant finishedAt, int newPersonalRecords) {}
