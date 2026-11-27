package com.ferro.identity.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void accepts_a_valid_password() {
        PasswordPolicy.validate("Ferro12345");
    }

    @Test
    void rejects_invalid_passwords() {
        assertThatThrownBy(() -> PasswordPolicy.validate(null)).isInstanceOf(InvalidPasswordException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("short")).isInstanceOf(InvalidPasswordException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("a".repeat(129))).isInstanceOf(InvalidPasswordException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("FERR012345")).isInstanceOf(InvalidPasswordException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("ferro12345")).isInstanceOf(InvalidPasswordException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("FerroPassword")).isInstanceOf(InvalidPasswordException.class);
    }
}
