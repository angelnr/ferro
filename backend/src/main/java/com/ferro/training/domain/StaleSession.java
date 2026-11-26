package com.ferro.training.domain;

import java.time.Instant;
import java.util.UUID;

public record StaleSession(
        UUID sessionId, UUID userId, String userEmail, String locale, Instant startedAt, Instant lastSetAt) {}
