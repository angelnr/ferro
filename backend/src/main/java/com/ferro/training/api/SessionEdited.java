package com.ferro.training.api;

import java.time.Instant;
import java.util.UUID;

public record SessionEdited(UUID sessionId, UUID userId, Instant editedAt) {}
