package com.ferro.identity.infrastructure.security;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ferro.identity.application.PasswordResetProperties;
import com.ferro.identity.application.TokenHasher;
import com.ferro.identity.application.port.PasswordResetTokenFactory;
import com.ferro.identity.domain.PasswordResetToken;

@Component
public class SecurePasswordResetTokenFactory implements PasswordResetTokenFactory {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final PasswordResetProperties properties;

    public SecurePasswordResetTokenFactory(PasswordResetProperties properties) {
        this.properties = properties;
    }

    @Override
    public IssuedPasswordResetToken create(UUID userId, Instant now) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        PasswordResetToken token =
                PasswordResetToken.issue(userId, TokenHasher.sha256Hex(rawToken), now, properties.ttl());
        return new IssuedPasswordResetToken(token, rawToken);
    }
}
