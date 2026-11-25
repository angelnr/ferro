package com.ferro.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    @Test
    void normalizes_unicode_case_and_surrounding_whitespace() {
        Email email = new Email("  P1@Ferro.DEV  ");

        assertThat(email.value()).isEqualTo("p1@ferro.dev");
    }

    @Test
    void applies_nfkc_normalization() {
        Email email = new Email("jose\u0301@ferro.dev");

        assertThat(email.value()).isEqualTo("jos\u00e9@ferro.dev");
    }

    @Test
    void equality_uses_normalized_value() {
        assertThat(new Email("P1@Ferro.DEV")).isEqualTo(new Email(" p1@ferro.dev "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "sin-arroba", "a@b", "a b@ferro.dev", "@ferro.dev", "a@.dev"})
    void rejects_invalid_emails(String raw) {
        assertThatThrownBy(() -> new Email(raw)).isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new Email(null)).isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void rejects_overlong_email() {
        String local = "a".repeat(250);

        assertThatThrownBy(() -> new Email(local + "@ferro.dev")).isInstanceOf(InvalidEmailException.class);
    }
}
