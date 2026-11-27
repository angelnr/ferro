package com.ferro.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-11-27T10:00:00Z");

    private User user() {
        return User.register(
                new Email("user@ferro.dev"), "hash", new DisplayName("User"), AppLocale.ES, UnitSystem.KG, NOW);
    }

    @Test
    void registers_unverified_with_defaults() {
        User user = user();
        assertThat(user.status()).isEqualTo(UserStatus.UNVERIFIED);
        assertThat(user.locale()).isEqualTo(AppLocale.ES);
        assertThat(user.timerSound()).isTrue();
        assertThat(user.emailWeeklySummary()).isFalse();
        assertThat(user.emailVerifiedAt()).isNull();
        assertThat(user.id()).isNotNull();
        assertThat(user.email().value()).isEqualTo("user@ferro.dev");
        assertThat(user.healthConsentAt()).isEqualTo(NOW);
        assertThat(user.createdAt()).isEqualTo(NOW);
        assertThat(user.updatedAt()).isEqualTo(NOW);
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.status()).isEqualTo(UserStatus.UNVERIFIED);
        assertThat(user.passwordHash()).isEqualTo("hash");
    }

    @Test
    void rehydrates_persisted_state() {
        User user = User.rehydrate(
                java.util.UUID.randomUUID(),
                new Email("user@ferro.dev"),
                "hash",
                new DisplayName("User"),
                UserStatus.ACTIVE,
                AppLocale.EN,
                UnitSystem.LB,
                Theme.LIGHT,
                "Europe/London",
                120,
                false,
                false,
                NOW,
                NOW,
                2,
                null,
                NOW,
                NOW);
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.emailVerifiedAt()).isEqualTo(NOW);
        assertThat(user.timeZone()).isEqualTo("Europe/London");
        assertThat(user.failedLoginCount()).isEqualTo(2);
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void verification_and_profile_updates_are_applied() {
        User user = user();
        user.verifyEmail(NOW);
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        user.updateProfile(new DisplayName("Nuevo"), AppLocale.EN, UnitSystem.LB, Theme.LIGHT, 120, false, true, NOW);
        assertThat(user.displayName().value()).isEqualTo("Nuevo");
        assertThat(user.locale()).isEqualTo(AppLocale.EN);
        assertThat(user.unitSystem()).isEqualTo(UnitSystem.LB);
        assertThat(user.theme()).isEqualTo(Theme.LIGHT);
        assertThat(user.defaultRestSeconds()).isEqualTo(120);
        assertThat(user.timerSound()).isFalse();
        assertThat(user.emailWeeklySummary()).isTrue();
    }

    @Test
    void invalid_profile_update_is_rejected() {
        User user = user();
        assertThatThrownBy(() -> user.updateProfile(null, null, null, null, 10, null, null, NOW))
                .isInstanceOf(InvalidProfileException.class);
        assertThatThrownBy(() -> user.updateProfile(null, null, null, null, null, null, null, NOW))
                .isInstanceOf(InvalidProfileException.class);
    }

    @Test
    void failed_logins_lock_and_successful_login_unlocks() {
        User user = user();
        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(NOW);
        }
        assertThat(user.status()).isEqualTo(UserStatus.LOCKED);
        assertThat(user.lockedUntil()).isAfter(NOW);
        user.recordSuccessfulLogin(NOW.plusSeconds(1));
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.failedLoginCount()).isZero();
    }

    @Test
    void expired_unverified_account_cannot_login() {
        User user = user();
        assertThatThrownBy(() -> user.ensureCanLogin(NOW.plusSeconds(25 * 3600)))
                .isInstanceOf(EmailUnverifiedException.class);
    }

    @Test
    void oauth_registration_and_password_change() {
        User oauth = User.registerOAuth(new Email("oauth@ferro.dev"), new DisplayName("OAuth"), true, NOW);
        assertThat(oauth.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(oauth.emailVerifiedAt()).isEqualTo(NOW);
        oauth.changePasswordHash("new-hash", NOW.plusSeconds(1));
        assertThat(oauth.passwordHash()).isEqualTo("new-hash");
    }
}
