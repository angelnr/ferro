package com.ferro.training.api;

import java.time.Instant;
import java.util.UUID;

public record SessionDiscarded(UUID sessionId, UUID userId, Instant discardedAt) {}
