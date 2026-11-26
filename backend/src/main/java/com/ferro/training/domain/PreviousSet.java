package com.ferro.training.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PreviousSet(int reps, BigDecimal weight, Integer rir, SetType setType, Instant loggedAt) {}
