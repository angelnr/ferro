package com.ferro.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TokenTest {

    private static final Instant NOW = Instant.parse("2026-11-27T10:00:00Z");

    @Test
    void refresh_tokens_expire_and_revoke() {
        RefreshToken token = RefreshToken.issue(UUID.randomUUID(), "hash", NOW, Duration.ofMinutes(10));
        assertThat(token.id()).isNotNull();
        assertThat(token.userId()).isNotNull();
        assertThat(token.tokenHash()).isEqualTo("hash");
        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
        assertThat(token.revokedAt()).isNull();
        assertThat(token.replacedBy()).isNull();
        assertThat(token.isActive(NOW)).isTrue();
        assertThat(token.isActive(NOW.plus(Duration.ofMinutes(11)))).isFalse();
        token.revoke(NOW, UUID.randomUUID());
        assertThat(token.isActive(NOW)).isFalse();
    }

    @Test
    void verification_and_reset_tokens_are_single_use() {
        EmailVerificationToken verification =
                EmailVerificationToken.issue(UUID.randomUUID(), "hash", NOW, Duration.ofHours(1));
        assertThat(verification.id()).isNotNull();
        assertThat(verification.userId()).isNotNull();
        assertThat(verification.tokenHash()).isEqualTo("hash");
        assertThat(verification.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        assertThat(verification.createdAt()).isEqualTo(NOW);
        assertThat(verification.usedAt()).isNull();
        assertThat(verification.isActive(NOW)).isTrue();
        verification.markUsed(NOW);
        assertThat(verification.isActive(NOW)).isFalse();
        assertThat(EmailVerificationToken.rehydrate(
                                verification.id(), verification.userId(), "hash", NOW.plusSeconds(60), null, NOW)
                        .isActive(NOW.plusSeconds(120)))
                .isFalse();

        PasswordResetToken reset = PasswordResetToken.issue(UUID.randomUUID(), "hash", NOW, Duration.ofMinutes(30));
        assertThat(reset.id()).isNotNull();
        assertThat(reset.userId()).isNotNull();
        assertThat(reset.tokenHash()).isEqualTo("hash");
        assertThat(reset.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        assertThat(reset.createdAt()).isEqualTo(NOW);
        assertThat(reset.usedAt()).isNull();
        assertThat(reset.isActive(NOW)).isTrue();
        reset.markUsed(NOW);
        assertThat(reset.isActive(NOW)).isFalse();
        assertThat(PasswordResetToken.rehydrate(reset.id(), reset.userId(), "hash", NOW.plusSeconds(60), null, NOW)
                        .isActive(NOW.plusSeconds(120)))
                .isFalse();
    }
}
