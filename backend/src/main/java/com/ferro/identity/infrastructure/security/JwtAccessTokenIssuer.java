package com.ferro.identity.infrastructure.security;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.ferro.identity.application.port.AccessTokenIssuer;

/** Access token JWT HS256 de 15 min con sub, jti, iat, exp y typ=access (ADR-0006). */
@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration accessTtl;

    public JwtAccessTokenIssuer(
            JwtEncoder encoder,
            @Value("${ferro.security.jwt.issuer:ferro}") String issuer,
            @Value("${ferro.security.jwt.access-ttl:15m}") Duration accessTtl) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.accessTtl = accessTtl;
    }

    @Override
    public String issue(UUID userId, Instant now) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(accessTtl))
                .id(UUID.randomUUID().toString())
                .claim("typ", "access")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
