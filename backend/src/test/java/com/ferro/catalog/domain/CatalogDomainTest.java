package com.ferro.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class CatalogDomainTest {

    @Test
    void calculates_page_count_and_empty_stats() {
        PageResult<String> page = new PageResult<>(List.of("a"), 0, 10, 25);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(new PageResult<String>(List.of(), 0, 10, 0).totalPages()).isZero();
        assertThat(ExerciseStats.empty().personalRecords()).isEmpty();
        assertThat(ExerciseStats.empty().totalVolume()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
