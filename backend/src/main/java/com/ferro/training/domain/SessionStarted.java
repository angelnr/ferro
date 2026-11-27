package com.ferro.training.domain;

import java.time.Instant;
import java.util.UUID;

public record SessionStarted(UUID sessionId, UUID userId, Instant startedAt) {}
