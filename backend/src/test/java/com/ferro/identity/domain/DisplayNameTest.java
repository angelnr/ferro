package com.ferro.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DisplayNameTest {

    @Test
    void normalizes_outer_spaces_only() {
        assertThat(new DisplayName("  Ángel  ").value()).isEqualTo("Ángel");
        assertThat(new DisplayName("Ángel  García").value()).isEqualTo("Ángel  García");
    }

    @Test
    void rejects_empty_and_too_long_names() {
        assertThatThrownBy(() -> new DisplayName(null)).isInstanceOf(InvalidDisplayNameException.class);
        assertThatThrownBy(() -> new DisplayName("   ")).isInstanceOf(InvalidDisplayNameException.class);
        assertThatThrownBy(() -> new DisplayName("a".repeat(81))).isInstanceOf(InvalidDisplayNameException.class);
    }
}
