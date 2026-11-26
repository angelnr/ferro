package com.ferro.training.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cálculos de RN-03/RN-04, compartidos por servicio y persistencia. */
public final class SetMath {

    private SetMath() {}

    public static BigDecimal volume(SetType type, BigDecimal weight, int reps) {
        if (type == SetType.WARMUP || weight == null || weight.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return weight.multiply(BigDecimal.valueOf(reps)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal e1rm(SetType type, BigDecimal weight, int reps) {
        if (type == SetType.WARMUP || weight == null || weight.signum() <= 0 || reps > 12) {
            return null;
        }
        return weight.multiply(BigDecimal.ONE.add(
                        BigDecimal.valueOf(reps).divide(BigDecimal.valueOf(30), 6, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
