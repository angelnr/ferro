package com.ferro.identity.application.port;

import java.time.Instant;
import java.util.UUID;

import com.ferro.identity.domain.PasswordResetToken;

public interface PasswordResetTokenFactory {

    IssuedPasswordResetToken create(UUID userId, Instant now);

    record IssuedPasswordResetToken(PasswordResetToken token, String rawToken) {}
}
