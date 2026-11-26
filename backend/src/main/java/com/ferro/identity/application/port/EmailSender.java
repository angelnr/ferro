package com.ferro.identity.application.port;

import com.ferro.identity.domain.Email;

/** Puerto de salida de emails. En dev se implementa contra Mailpit. */
public interface EmailSender {

    void sendVerificationEmail(Email recipient, String verificationLink);

    void sendPasswordResetEmail(Email recipient, String resetLink);

    void sendPasswordChangedEmail(Email recipient);

    void sendAccountDeletedEmail(Email recipient);
}
