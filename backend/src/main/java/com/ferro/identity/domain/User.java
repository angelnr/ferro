package com.ferro.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.ferro.shared.domain.UuidV7;

/**
 * Cuenta de usuario. Agregado raíz del bounded context {@code identity}.
 *
 * <p>Contiene solo reglas de negocio; la persistencia vive en infrastructure.
 */
public final class User {

    private static final int MAX_FAILED_LOGINS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration VERIFICATION_WINDOW = Duration.ofHours(24);

    private final UUID id;
    private final Email email;
    private String passwordHash;
    private DisplayName displayName;
    private UserStatus status;
    private AppLocale locale;
    private UnitSystem unitSystem;
    private Theme theme;
    private String timeZone;
    private int defaultRestSeconds;
    private boolean timerSound;
    private Instant emailVerifiedAt;
    private final Instant healthConsentAt;
    private int failedLoginCount;
    private Instant lockedUntil;
    private final Instant createdAt;
    private Instant updatedAt;

    private User(
            UUID id,
            Email email,
            String passwordHash,
            DisplayName displayName,
            UserStatus status,
            AppLocale locale,
            UnitSystem unitSystem,
            Theme theme,
            String timeZone,
            int defaultRestSeconds,
            boolean timerSound,
            Instant emailVerifiedAt,
            Instant healthConsentAt,
            int failedLoginCount,
            Instant lockedUntil,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.status = status;
        this.locale = locale;
        this.unitSystem = unitSystem;
        this.theme = theme;
        this.timeZone = timeZone;
        this.defaultRestSeconds = defaultRestSeconds;
        this.timerSound = timerSound;
        this.emailVerifiedAt = emailVerifiedAt;
        this.healthConsentAt = healthConsentAt;
        this.failedLoginCount = failedLoginCount;
        this.lockedUntil = lockedUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User register(
            Email email,
            String passwordHash,
            DisplayName displayName,
            AppLocale locale,
            UnitSystem unitSystem,
            Instant now) {
        return new User(
                UuidV7.generate(),
                email,
                passwordHash,
                displayName,
                UserStatus.UNVERIFIED,
                locale,
                unitSystem,
                Theme.DARK,
                "Europe/Madrid",
                90,
                true,
                null,
                now,
                0,
                null,
                now,
                now);
    }

    public static User registerOAuth(Email email, DisplayName displayName, boolean emailVerified, Instant now) {
        return new User(
                UuidV7.generate(),
                email,
                null,
                displayName,
                emailVerified ? UserStatus.ACTIVE : UserStatus.UNVERIFIED,
                AppLocale.ES,
                UnitSystem.KG,
                Theme.DARK,
                "Europe/Madrid",
                90,
                true,
                emailVerified ? now : null,
                now,
                0,
                null,
                now,
                now);
    }

    public static User rehydrate(
            UUID id,
            Email email,
            String passwordHash,
            DisplayName displayName,
            UserStatus status,
            AppLocale locale,
            UnitSystem unitSystem,
            Theme theme,
            String timeZone,
            int defaultRestSeconds,
            boolean timerSound,
            Instant emailVerifiedAt,
            Instant healthConsentAt,
            int failedLoginCount,
            Instant lockedUntil,
            Instant createdAt,
            Instant updatedAt) {
        return new User(
                id,
                email,
                passwordHash,
                displayName,
                status,
                locale,
                unitSystem,
                theme,
                timeZone,
                defaultRestSeconds,
                timerSound,
                emailVerifiedAt,
                healthConsentAt,
                failedLoginCount,
                lockedUntil,
                createdAt,
                updatedAt);
    }

    /** RF-002: bloqueo progresivo y ventana de verificación de email (RN-16). */
    public void ensureCanLogin(Instant now) {
        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            throw new AccountLockedException(lockedUntil);
        }
        if (status == UserStatus.LOCKED) {
            status = UserStatus.ACTIVE;
            lockedUntil = null;
            updatedAt = now;
        }
        if (status == UserStatus.UNVERIFIED
                && createdAt.plus(VERIFICATION_WINDOW).isBefore(now)) {
            throw new EmailUnverifiedException();
        }
    }

    public void recordFailedLogin(Instant now) {
        failedLoginCount++;
        if (failedLoginCount >= MAX_FAILED_LOGINS) {
            status = UserStatus.LOCKED;
            lockedUntil = now.plus(LOCK_DURATION);
        }
        updatedAt = now;
    }

    public void recordSuccessfulLogin(Instant now) {
        failedLoginCount = 0;
        lockedUntil = null;
        if (status == UserStatus.LOCKED) {
            status = UserStatus.ACTIVE;
        }
        updatedAt = now;
    }

    public void verifyEmail(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
            status = UserStatus.ACTIVE;
            updatedAt = now;
        }
    }

    public void updateProfile(
            DisplayName newDisplayName,
            AppLocale newLocale,
            UnitSystem newUnitSystem,
            Theme newTheme,
            Integer newDefaultRestSeconds,
            Instant now) {
        if (newDisplayName == null
                && newLocale == null
                && newUnitSystem == null
                && newTheme == null
                && newDefaultRestSeconds == null) {
            throw new InvalidProfileException("No hay cambios que aplicar");
        }
        if (newDefaultRestSeconds != null && (newDefaultRestSeconds < 15 || newDefaultRestSeconds > 600)) {
            throw new InvalidProfileException("El descanso por defecto debe estar entre 15 y 600 segundos");
        }
        if (newDisplayName != null) {
            displayName = newDisplayName;
        }
        if (newLocale != null) {
            locale = newLocale;
        }
        if (newUnitSystem != null) {
            unitSystem = newUnitSystem;
        }
        if (newTheme != null) {
            theme = newTheme;
        }
        if (newDefaultRestSeconds != null) {
            defaultRestSeconds = newDefaultRestSeconds;
        }
        updatedAt = now;
    }

    public void changePasswordHash(String newPasswordHash, Instant now) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = now;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public UUID id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public DisplayName displayName() {
        return displayName;
    }

    public UserStatus status() {
        return status;
    }

    public AppLocale locale() {
        return locale;
    }

    public UnitSystem unitSystem() {
        return unitSystem;
    }

    public Theme theme() {
        return theme;
    }

    public String timeZone() {
        return timeZone;
    }

    public int defaultRestSeconds() {
        return defaultRestSeconds;
    }

    public boolean timerSound() {
        return timerSound;
    }

    public Instant emailVerifiedAt() {
        return emailVerifiedAt;
    }

    public Instant healthConsentAt() {
        return healthConsentAt;
    }

    public int failedLoginCount() {
        return failedLoginCount;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
