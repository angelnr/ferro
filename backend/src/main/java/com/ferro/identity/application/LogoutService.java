package com.ferro.identity.application;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.identity.application.port.RefreshTokenRepository;
import com.ferro.identity.domain.RefreshToken;

/** RF-004: logout del dispositivo actual. Idempotente: un token ausente también devuelve 204. */
@Service
public class LogoutService {

    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    public LogoutService(RefreshTokenRepository refreshTokens, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        RefreshToken token =
                refreshTokens.findByTokenHash(TokenHasher.sha256Hex(rawToken)).orElse(null);
        if (token == null || token.revokedAt() != null) {
            return;
        }
        token.revoke(clock.instant(), null);
        refreshTokens.save(token);
    }
}
