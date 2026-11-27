package com.ferro.training.api;

import java.time.Instant;
import java.util.UUID;

public record SessionStarted(UUID sessionId, UUID userId, Instant startedAt) {}
