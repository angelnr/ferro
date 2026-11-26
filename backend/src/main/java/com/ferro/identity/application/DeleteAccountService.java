package com.ferro.identity.application;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.identity.application.port.EmailSender;
import com.ferro.identity.application.port.PasswordHasher;
import com.ferro.identity.application.port.UserRepository;
import com.ferro.identity.domain.Email;
import com.ferro.identity.domain.InvalidCredentialsException;
import com.ferro.identity.domain.User;

/** RF-010/RN-18: borrado real en cascada tras re-autenticación. */
@Service
public class DeleteAccountService {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final EmailSender emailSender;
    private final Clock clock;

    public DeleteAccountService(
            UserRepository users, PasswordHasher passwordHasher, EmailSender emailSender, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public void delete(UUID userId, String confirmPassword) {
        User user = users.findById(userId).orElseThrow(InvalidCredentialsException::new);
        if (user.passwordHash() != null
                && (confirmPassword == null || !passwordHasher.matches(confirmPassword, user.passwordHash()))) {
            user.recordFailedLogin(clock.instant());
            users.save(user);
            throw new InvalidCredentialsException();
        }

        Email email = user.email();
        users.delete(user);
        emailSender.sendAccountDeletedEmail(email);
    }
}
