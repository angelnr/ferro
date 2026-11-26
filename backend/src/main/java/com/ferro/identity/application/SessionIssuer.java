package com.ferro.identity.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.ferro.identity.application.port.AccessTokenIssuer;
import com.ferro.identity.application.port.RefreshTokenFactory;
import com.ferro.identity.application.port.RefreshTokenRepository;
import com.ferro.identity.domain.RefreshToken;
import com.ferro.identity.domain.User;

/**
 * Emite y rota pares de tokens. La rotación inserta primero el token nuevo porque
 * {@code refresh_tokens.replaced_by_id} es una FK al mismo agregado.
 */
@Service
public class SessionIssuer {

    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenFactory refreshTokenFactory;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    public SessionIssuer(
            AccessTokenIssuer accessTokenIssuer,
            RefreshTokenFactory refreshTokenFactory,
            RefreshTokenRepository refreshTokens,
            Clock clock) {
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenFactory = refreshTokenFactory;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    public AuthResult issue(User user) {
        Instant now = clock.instant();
        RefreshTokenFactory.IssuedRefreshToken issued = refreshTokenFactory.create(user.id(), now);
        refreshTokens.save(issued.token());
        return new AuthResult(user, accessTokenIssuer.issue(user.id(), now), issued.rawToken());
    }

    public AuthResult rotate(User user, RefreshToken current) {
        Instant now = clock.instant();
        RefreshTokenFactory.IssuedRefreshToken issued = refreshTokenFactory.create(user.id(), now);
        refreshTokens.save(issued.token());
        current.revoke(now, issued.token().id());
        refreshTokens.save(current);
        return new AuthResult(user, accessTokenIssuer.issue(user.id(), now), issued.rawToken());
    }
}
