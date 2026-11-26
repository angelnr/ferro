package com.ferro.training.domain;

import java.time.Instant;
import java.util.UUID;

public record StaleSession(UUID sessionId, UUID userId, String userEmail, Instant startedAt, Instant lastSetAt) {}
