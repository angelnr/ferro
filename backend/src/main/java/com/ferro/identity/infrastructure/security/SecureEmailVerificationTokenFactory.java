package com.ferro.identity.infrastructure.security;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ferro.identity.application.EmailVerificationProperties;
import com.ferro.identity.application.TokenHasher;
import com.ferro.identity.application.port.EmailVerificationTokenFactory;
import com.ferro.identity.domain.EmailVerificationToken;

@Component
public class SecureEmailVerificationTokenFactory implements EmailVerificationTokenFactory {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final EmailVerificationProperties properties;

    public SecureEmailVerificationTokenFactory(EmailVerificationProperties properties) {
        this.properties = properties;
    }

    @Override
    public IssuedVerificationToken create(UUID userId, Instant now) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        EmailVerificationToken token =
                EmailVerificationToken.issue(userId, TokenHasher.sha256Hex(rawToken), now, properties.ttl());
        return new IssuedVerificationToken(token, rawToken);
    }
}
