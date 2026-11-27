package com.ferro.training.api;

import java.time.Instant;
import java.util.UUID;

public record SetLogged(UUID sessionId, UUID userId, UUID setId, Instant loggedAt) {}
