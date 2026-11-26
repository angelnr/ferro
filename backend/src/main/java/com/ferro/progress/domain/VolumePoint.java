package com.ferro.progress.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record VolumePoint(Instant date, BigDecimal volume) {}
