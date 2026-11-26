package com.ferro.identity.application;

import java.time.Clock;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.identity.application.port.CommonPasswordChecker;
import com.ferro.identity.application.port.EmailSender;
import com.ferro.identity.application.port.PasswordHasher;
import com.ferro.identity.application.port.UserRepository;
import com.ferro.identity.domain.AppLocale;
import com.ferro.identity.domain.DisplayName;
import com.ferro.identity.domain.Email;
import com.ferro.identity.domain.EmailTakenException;
import com.ferro.identity.domain.HealthConsentRequiredException;
import com.ferro.identity.domain.InvalidPasswordException;
import com.ferro.identity.domain.InvalidUnitSystemException;
import com.ferro.identity.domain.PasswordPolicy;
import com.ferro.identity.domain.UnitSystem;
import com.ferro.identity.domain.User;

/** RF-001: registro email+contraseña. Email, verificación y rate limit llegan en el siguiente PR. */
@Service
public class RegisterUserService {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final SessionIssuer sessions;
    private final EmailVerificationService emailVerifications;
    private final EmailSender emailSender;
    private final CommonPasswordChecker commonPasswords;
    private final Clock clock;

    public RegisterUserService(
            UserRepository users,
            PasswordHasher passwordHasher,
            SessionIssuer sessions,
            EmailVerificationService emailVerifications,
            EmailSender emailSender,
            CommonPasswordChecker commonPasswords,
            Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.sessions = sessions;
        this.emailVerifications = emailVerifications;
        this.emailSender = emailSender;
        this.commonPasswords = commonPasswords;
        this.clock = clock;
    }

    @Transactional
    public AuthResult register(RegisterCommand command) {
        if (!command.healthConsent()) {
            throw new HealthConsentRequiredException();
        }
        Email email = new Email(command.email());
        DisplayName displayName = new DisplayName(command.displayName());
        PasswordPolicy.validate(command.rawPassword());
        if (commonPasswords.isCommon(command.rawPassword())) {
            throw new InvalidPasswordException("La contraseña es demasiado común");
        }

        if (users.existsByEmail(email)) {
            throw new EmailTakenException();
        }

        User user = User.register(
                email,
                passwordHasher.hash(command.rawPassword()),
                displayName,
                AppLocale.fromCode(command.locale()),
                parseUnitSystem(command.unitSystem()),
                clock.instant());

        try {
            users.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailTakenException();
        }
        emailVerifications.issueFor(user);
        emailSender.sendWelcomeEmail(user.email());
        return sessions.issue(user);
    }

    private UnitSystem parseUnitSystem(String value) {
        if (value == null || value.isBlank()) {
            return UnitSystem.KG;
        }
        try {
            return UnitSystem.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new InvalidUnitSystemException("Unidad no soportada: " + value);
        }
    }
}
