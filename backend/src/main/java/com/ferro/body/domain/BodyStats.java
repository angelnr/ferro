package com.ferro.body.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BodyStats(
        List<Point> weight, List<Point> chest, List<Point> waist, List<Point> hip, List<Point> arm, List<Point> thigh) {

    public record Point(Instant date, BigDecimal value) {}
}
