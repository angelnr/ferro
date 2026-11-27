package com.ferro.identity.application;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.identity.application.port.UserRepository;
import com.ferro.identity.domain.AppLocale;
import com.ferro.identity.domain.DisplayName;
import com.ferro.identity.domain.InvalidProfileException;
import com.ferro.identity.domain.Theme;
import com.ferro.identity.domain.UnitSystem;
import com.ferro.identity.domain.User;

/** RF-007: actualización parcial de perfil y preferencias. */
@Service
public class UpdateProfileService {

    private final UserRepository users;
    private final Clock clock;

    public UpdateProfileService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public void update(UUID userId, UpdateProfileCommand command) {
        User user = users.findById(userId).orElseThrow(() -> new InvalidProfileException("Usuario no encontrado"));
        user.updateProfile(
                parseDisplayName(command.displayName()),
                parseLocale(command.locale()),
                parseUnitSystem(command.unitSystem()),
                parseTheme(command.theme()),
                command.defaultRestSeconds(),
                command.timerSound(),
                command.emailWeeklySummary(),
                clock.instant());
        users.save(user);
    }

    private DisplayName parseDisplayName(String value) {
        return value == null ? null : new DisplayName(value);
    }

    private AppLocale parseLocale(String value) {
        return value == null ? null : AppLocale.fromCode(value);
    }

    private UnitSystem parseUnitSystem(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UnitSystem.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new InvalidProfileException("Unidad no soportada: " + value);
        }
    }

    private Theme parseTheme(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Theme.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new InvalidProfileException("Tema no soportado: " + value);
        }
    }
}
