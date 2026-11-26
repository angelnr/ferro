package com.ferro.identity.application;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.identity.application.port.CommonPasswordChecker;
import com.ferro.identity.application.port.EmailSender;
import com.ferro.identity.application.port.PasswordHasher;
import com.ferro.identity.application.port.RefreshTokenRepository;
import com.ferro.identity.application.port.UserRepository;
import com.ferro.identity.domain.InvalidCredentialsException;
import com.ferro.identity.domain.InvalidPasswordException;
import com.ferro.identity.domain.PasswordPolicy;
import com.ferro.identity.domain.User;

/** RF-008: cambio de contraseña con re-autenticación y revocación selectiva de sesiones. */
@Service
public class ChangePasswordService {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final CommonPasswordChecker commonPasswords;
    private final RefreshTokenRepository refreshTokens;
    private final EmailSender emailSender;
    private final Clock clock;

    public ChangePasswordService(
            UserRepository users,
            PasswordHasher passwordHasher,
            CommonPasswordChecker commonPasswords,
            RefreshTokenRepository refreshTokens,
            EmailSender emailSender,
            Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.commonPasswords = commonPasswords;
        this.refreshTokens = refreshTokens;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public void change(UUID userId, String currentPassword, String newPassword, String rawRefreshToken) {
        User user = users.findById(userId).orElseThrow(InvalidCredentialsException::new);
        if (user.passwordHash() == null || !passwordHasher.matches(currentPassword, user.passwordHash())) {
            user.recordFailedLogin(clock.instant());
            users.save(user);
            throw new InvalidCredentialsException();
        }

        PasswordPolicy.validate(newPassword);
        if (commonPasswords.isCommon(newPassword)) {
            throw new InvalidPasswordException("La contraseña es demasiado común");
        }

        user.changePasswordHash(passwordHasher.hash(newPassword), clock.instant());
        users.save(user);
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokens.revokeAllForUserExcept(userId, TokenHasher.sha256Hex(rawRefreshToken), clock.instant());
        } else {
            refreshTokens.revokeAllForUser(userId, clock.instant());
        }
        emailSender.sendPasswordChangedEmail(user.email(), user.locale().code());
    }
}
