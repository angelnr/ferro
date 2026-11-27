package com.ferro.training.domain;

import java.time.Instant;
import java.util.UUID;

public record SessionDiscarded(UUID sessionId, UUID userId, Instant discardedAt) {}
