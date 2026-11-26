package com.ferro.identity.application.oauth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.ferro.identity.domain.OAuthLoginException;
import com.ferro.identity.domain.OAuthProvider;

/** Estado OAuth firmado (JWT de 5 min) que transporta el code_verifier de PKCE sin BD. */
@Service
public class OAuthStateService {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    private final String issuer;

    public OAuthStateService(
            JwtEncoder encoder,
            JwtDecoder decoder,
            Clock clock,
            @Value("${ferro.security.jwt.issuer:ferro}") String issuer) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.clock = clock;
        this.issuer = issuer;
    }

    public String create(OAuthProvider provider, String codeVerifier, String redirectUri) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("oauth-state")
                .issuedAt(now)
                .expiresAt(now.plus(TTL))
                .id(UUID.randomUUID().toString())
                .claim("typ", "oauth_state")
                .claim("provider", provider.name())
                .claim("verifier", codeVerifier)
                .claim("redirect_uri", redirectUri)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public OAuthState parse(String state) {
        try {
            Jwt jwt = decoder.decode(state);
            if (!"oauth_state".equals(jwt.getClaimAsString("typ"))) {
                throw new OAuthLoginException("State OAuth inválido");
            }
            return new OAuthState(
                    OAuthProvider.fromPath(jwt.getClaimAsString("provider")),
                    jwt.getClaimAsString("verifier"),
                    jwt.getClaimAsString("redirect_uri"),
                    jwt.getExpiresAt());
        } catch (OAuthLoginException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new OAuthLoginException("State OAuth inválido o caducado");
        }
    }

    public String createEmailOnboarding(OAuthProvider provider, String subject, String displayName) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("oauth-email")
                .issuedAt(now)
                .expiresAt(now.plus(TTL))
                .id(UUID.randomUUID().toString())
                .claim("typ", "oauth_email")
                .claim("provider", provider.name())
                .claim("provider_subject", subject)
                .claim("display_name", displayName == null ? "" : displayName)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public OAuthOnboarding parseEmailOnboarding(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            if (!"oauth_email".equals(jwt.getClaimAsString("typ"))) {
                throw new OAuthLoginException("Token de onboarding inválido");
            }
            return new OAuthOnboarding(
                    OAuthProvider.fromPath(jwt.getClaimAsString("provider")),
                    jwt.getClaimAsString("provider_subject"),
                    jwt.getClaimAsString("display_name"));
        } catch (OAuthLoginException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new OAuthLoginException("Token de onboarding inválido o caducado");
        }
    }
}
