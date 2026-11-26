package com.ferro.identity.infrastructure.security;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ferro.identity.application.RefreshProperties;
import com.ferro.identity.application.TokenHasher;
import com.ferro.identity.application.port.RefreshTokenFactory;
import com.ferro.identity.domain.RefreshToken;

/** Refresh opaco de 256 bits; en BD solo viaja su SHA-256. */
@Component
public class SecureRefreshTokenFactory implements RefreshTokenFactory {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final RefreshProperties properties;

    public SecureRefreshTokenFactory(RefreshProperties properties) {
        this.properties = properties;
    }

    @Override
    public IssuedRefreshToken create(UUID userId, Instant now) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken token = RefreshToken.issue(userId, TokenHasher.sha256Hex(rawToken), now, properties.ttl());
        return new IssuedRefreshToken(token, rawToken);
    }
}
