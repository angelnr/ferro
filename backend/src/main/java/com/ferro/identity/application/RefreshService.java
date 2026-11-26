package com.ferro.identity.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.identity.application.port.RefreshTokenRepository;
import com.ferro.identity.application.port.UserRepository;
import com.ferro.identity.domain.RefreshInvalidException;
import com.ferro.identity.domain.RefreshToken;
import com.ferro.identity.domain.User;

/**
 * RF-003/RN-15: rotación de refresh con detección de reutilización.
 *
 * <p>{@code noRollbackFor} es imprescindible: si el token está revocado hay que persistir la
 * revocación de toda la familia aunque el endpoint termine en 401.
 */
@Service
public class RefreshService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final SessionIssuer sessions;
    private final Clock clock;

    public RefreshService(
            UserRepository users, RefreshTokenRepository refreshTokens, SessionIssuer sessions, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = RefreshInvalidException.class)
    public AuthResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new RefreshInvalidException();
        }

        Instant now = clock.instant();
        RefreshToken token = refreshTokens
                .findByTokenHash(TokenHasher.sha256Hex(rawToken))
                .orElseThrow(RefreshInvalidException::new);

        if (token.revokedAt() != null) {
            refreshTokens.revokeAllForUser(token.userId(), now);
            throw new RefreshInvalidException();
        }
        if (!token.isActive(now)) {
            throw new RefreshInvalidException();
        }

        User user = users.findById(token.userId()).orElseThrow(RefreshInvalidException::new);
        return sessions.rotate(user, token);
    }
}
