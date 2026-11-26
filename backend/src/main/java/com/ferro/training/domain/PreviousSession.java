package com.ferro.training.domain;

import java.time.Instant;

public record PreviousSession(java.util.UUID id, Instant startedAt, java.util.List<PreviousSet> sets) {}
