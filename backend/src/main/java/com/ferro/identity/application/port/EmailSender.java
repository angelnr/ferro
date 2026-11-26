package com.ferro.identity.application.port;

import com.ferro.identity.domain.Email;

/** Puerto de salida de emails. En dev se implementa contra Mailpit. */
public interface EmailSender {

    void sendVerificationEmail(Email recipient, String locale, String verificationLink);

    void sendPasswordResetEmail(Email recipient, String locale, String resetLink);

    void sendPasswordChangedEmail(Email recipient, String locale);

    void sendAccountDeletedEmail(Email recipient, String locale);

    void sendWelcomeEmail(Email recipient, String locale);

    void sendSecurityAlertEmail(Email recipient, String locale);
}
