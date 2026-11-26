package com.ferro.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.identity.application.port.EmailSender;
import com.ferro.identity.application.port.EmailVerificationTokenFactory;
import com.ferro.identity.application.port.EmailVerificationTokenRepository;
import com.ferro.identity.application.port.UserRepository;
import com.ferro.identity.domain.EmailVerificationToken;
import com.ferro.identity.domain.User;
import com.ferro.identity.domain.VerificationInvalidException;

/** Emisión, reenvío y confirmación del token de verificación de email (RN-16). */
@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokens;
    private final EmailVerificationTokenFactory tokenFactory;
    private final UserRepository users;
    private final EmailSender emailSender;
    private final AppProperties appProperties;
    private final Clock clock;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokens,
            EmailVerificationTokenFactory tokenFactory,
            UserRepository users,
            EmailSender emailSender,
            AppProperties appProperties,
            Clock clock) {
        this.tokens = tokens;
        this.tokenFactory = tokenFactory;
        this.users = users;
        this.emailSender = emailSender;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    public void issueFor(User user) {
        Instant now = clock.instant();
        tokens.invalidateActiveForUser(user.id(), now);
        EmailVerificationTokenFactory.IssuedVerificationToken issued = tokenFactory.create(user.id(), now);
        tokens.save(issued.token());
        String link = appProperties.frontendUrl().replaceAll("/+$", "") + "/auth/verify?token=" + issued.rawToken();
        emailSender.sendVerificationEmail(user.email(), link);
    }

    @Transactional
    public void resend(UUID userId) {
        User user = users.findById(userId).orElseThrow(VerificationInvalidException::new);
        if (user.isEmailVerified()) {
            return;
        }
        issueFor(user);
    }

    @Transactional
    public void confirm(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new VerificationInvalidException();
        }
        Instant now = clock.instant();
        EmailVerificationToken token =
                tokens.findByTokenHash(TokenHasher.sha256Hex(rawToken)).orElseThrow(VerificationInvalidException::new);
        if (!token.isActive(now)) {
            throw new VerificationInvalidException();
        }

        User user = users.findById(token.userId()).orElseThrow(VerificationInvalidException::new);
        token.markUsed(now);
        user.verifyEmail(now);
        tokens.save(token);
        users.save(user);
    }
}
