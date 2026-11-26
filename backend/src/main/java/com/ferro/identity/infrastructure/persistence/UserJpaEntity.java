package com.ferro.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ferro.identity.domain.AppLocale;
import com.ferro.identity.domain.DisplayName;
import com.ferro.identity.domain.Email;
import com.ferro.identity.domain.Theme;
import com.ferro.identity.domain.UnitSystem;
import com.ferro.identity.domain.User;
import com.ferro.identity.domain.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false, length = 2)
    private String locale;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "unit_system", nullable = false)
    private UnitSystem unitSystem;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private Theme theme;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(name = "default_rest_seconds", nullable = false)
    private int defaultRestSeconds;

    @Column(name = "timer_sound", nullable = false)
    private boolean timerSound;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "health_consent_at", nullable = false)
    private Instant healthConsentAt;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserJpaEntity() {}

    private UserJpaEntity(User user) {
        this.id = user.id();
        this.email = user.email().value();
        this.passwordHash = user.passwordHash();
        this.displayName = user.displayName().value();
        this.status = user.status();
        this.locale = user.locale().code();
        this.unitSystem = user.unitSystem();
        this.theme = user.theme();
        this.timeZone = user.timeZone();
        this.defaultRestSeconds = user.defaultRestSeconds();
        this.timerSound = user.timerSound();
        this.emailVerifiedAt = user.emailVerifiedAt();
        this.healthConsentAt = user.healthConsentAt();
        this.failedLoginCount = user.failedLoginCount();
        this.lockedUntil = user.lockedUntil();
        this.createdAt = user.createdAt();
        this.updatedAt = user.updatedAt();
    }

    public static UserJpaEntity from(User user) {
        return new UserJpaEntity(user);
    }

    public User toDomain() {
        return User.rehydrate(
                id,
                new Email(email),
                passwordHash,
                new DisplayName(displayName),
                status,
                AppLocale.fromCode(locale),
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

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}
