package com.ferro.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class SetMathTest {

    @Test
    void volume_ignores_warmup_and_non_positive_weight() {
        assertThat(SetMath.volume(SetType.WARMUP, BigDecimal.TEN, 5)).isEqualByComparingTo("0.00");
        assertThat(SetMath.volume(SetType.NORMAL, BigDecimal.ZERO, 5)).isEqualByComparingTo("0.00");
        assertThat(SetMath.volume(SetType.NORMAL, BigDecimal.valueOf(-2), 5)).isEqualByComparingTo("0.00");
        assertThat(SetMath.volume(SetType.NORMAL, BigDecimal.TEN, 5)).isEqualByComparingTo("50.00");
    }

    @Test
    void e1rm_is_null_for_invalid_or_high_rep_sets() {
        assertThat(SetMath.e1rm(SetType.WARMUP, BigDecimal.TEN, 5)).isNull();
        assertThat(SetMath.e1rm(SetType.NORMAL, BigDecimal.ZERO, 5)).isNull();
        assertThat(SetMath.e1rm(SetType.NORMAL, BigDecimal.TEN, 13)).isNull();
        assertThat(SetMath.e1rm(SetType.NORMAL, BigDecimal.valueOf(100), 10)).isEqualByComparingTo("133.33");
    }
}
