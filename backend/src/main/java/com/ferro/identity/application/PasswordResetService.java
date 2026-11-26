package com.ferro.identity.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.identity.application.port.CommonPasswordChecker;
import com.ferro.identity.application.port.EmailSender;
import com.ferro.identity.application.port.PasswordHasher;
import com.ferro.identity.application.port.PasswordResetTokenFactory;
import com.ferro.identity.application.port.PasswordResetTokenRepository;
import com.ferro.identity.application.port.RefreshTokenRepository;
import com.ferro.identity.application.port.UserRepository;
import com.ferro.identity.domain.Email;
import com.ferro.identity.domain.InvalidEmailException;
import com.ferro.identity.domain.InvalidPasswordException;
import com.ferro.identity.domain.PasswordPolicy;
import com.ferro.identity.domain.PasswordResetInvalidException;
import com.ferro.identity.domain.PasswordResetToken;
import com.ferro.identity.domain.User;

/** RF-009: recuperación de contraseña sin revelar si el email existe. */
@Service
public class PasswordResetService {

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordResetTokenFactory tokenFactory;
    private final PasswordHasher passwordHasher;
    private final CommonPasswordChecker commonPasswords;
    private final RefreshTokenRepository refreshTokens;
    private final EmailSender emailSender;
    private final AppProperties appProperties;
    private final Clock clock;

    public PasswordResetService(
            UserRepository users,
            PasswordResetTokenRepository tokens,
            PasswordResetTokenFactory tokenFactory,
            PasswordHasher passwordHasher,
            CommonPasswordChecker commonPasswords,
            RefreshTokenRepository refreshTokens,
            EmailSender emailSender,
            AppProperties appProperties,
            Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.tokenFactory = tokenFactory;
        this.passwordHasher = passwordHasher;
        this.commonPasswords = commonPasswords;
        this.refreshTokens = refreshTokens;
        this.emailSender = emailSender;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Transactional
    public void request(String rawEmail) {
        Email email;
        try {
            email = new Email(rawEmail);
        } catch (InvalidEmailException exception) {
            return; // 202 igualmente: no revelamos nada.
        }
        users.findByEmail(email).ifPresent(this::issueFor);
    }

    @Transactional
    public void confirm(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new PasswordResetInvalidException();
        }
        Instant now = clock.instant();
        PasswordResetToken token =
                tokens.findByTokenHash(TokenHasher.sha256Hex(rawToken)).orElseThrow(PasswordResetInvalidException::new);
        if (!token.isActive(now)) {
            throw new PasswordResetInvalidException();
        }

        PasswordPolicy.validate(newPassword);
        if (commonPasswords.isCommon(newPassword)) {
            throw new InvalidPasswordException("La contraseña es demasiado común");
        }

        User user = users.findById(token.userId()).orElseThrow(PasswordResetInvalidException::new);
        user.changePasswordHash(passwordHasher.hash(newPassword), now);
        token.markUsed(now);
        users.save(user);
        tokens.save(token);
        refreshTokens.revokeAllForUser(user.id(), now);
        emailSender.sendPasswordChangedEmail(user.email(), user.locale().code());
    }

    private void issueFor(User user) {
        Instant now = clock.instant();
        tokens.invalidateActiveForUser(user.id(), now);
        PasswordResetTokenFactory.IssuedPasswordResetToken issued = tokenFactory.create(user.id(), now);
        tokens.save(issued.token());
        String link =
                appProperties.frontendUrl().replaceAll("/+$", "") + "/auth/reset-password?token=" + issued.rawToken();
        emailSender.sendPasswordResetEmail(user.email(), user.locale().code(), link);
    }
}
