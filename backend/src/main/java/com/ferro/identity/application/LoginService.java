package com.ferro.identity.application;

import java.time.Clock;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.identity.application.port.PasswordHasher;
import com.ferro.identity.application.port.UserRepository;
import com.ferro.identity.domain.AccountLockedException;
import com.ferro.identity.domain.Email;
import com.ferro.identity.domain.EmailUnverifiedException;
import com.ferro.identity.domain.InvalidCredentialsException;
import com.ferro.identity.domain.InvalidEmailException;
import com.ferro.identity.domain.User;

/** RF-002: login con bloqueo progresivo. El refresh no revoca sesiones de otros dispositivos. */
@Service
public class LoginService {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final SessionIssuer sessions;
    private final Clock clock;

    public LoginService(UserRepository users, PasswordHasher passwordHasher, SessionIssuer sessions, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional(
            noRollbackFor = {
                InvalidCredentialsException.class,
                AccountLockedException.class,
                EmailUnverifiedException.class
            })
    public AuthResult login(LoginCommand command) {
        Email email;
        try {
            email = new Email(command.email());
        } catch (InvalidEmailException exception) {
            throw new InvalidCredentialsException();
        }

        Optional<User> found = users.findByEmail(email);
        if (found.isEmpty()) {
            throw new InvalidCredentialsException();
        }

        User user = found.get();
        user.ensureCanLogin(clock.instant());

        if (user.passwordHash() == null || !passwordHasher.matches(command.rawPassword(), user.passwordHash())) {
            user.recordFailedLogin(clock.instant());
            users.save(user);
            throw new InvalidCredentialsException();
        }

        user.recordSuccessfulLogin(clock.instant());
        users.save(user);
        return sessions.issue(user);
    }
}
