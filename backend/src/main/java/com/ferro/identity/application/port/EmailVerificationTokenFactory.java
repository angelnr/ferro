package com.ferro.identity.application.port;

import java.time.Instant;
import java.util.UUID;

import com.ferro.identity.domain.EmailVerificationToken;

public interface EmailVerificationTokenFactory {

    IssuedVerificationToken create(UUID userId, Instant now);

    record IssuedVerificationToken(EmailVerificationToken token, String rawToken) {}
}
