package com.ferro.identity.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.ferro.identity.application.port.EmailSender;
import com.ferro.identity.domain.Email;
import com.ferro.platform.api.EmailNotifier;
import com.ferro.platform.api.FerroMetrics;

/** Envío de emails transaccionales; en dev apunta a Mailpit (localhost:1025). */
@Component
public class MailEmailSender implements EmailSender, EmailNotifier {

    private static final Logger log = LoggerFactory.getLogger(MailEmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final FerroMetrics metrics;

    public MailEmailSender(
            JavaMailSender mailSender,
            @Value("${ferro.mail.from:no-reply@ferro.dev}") String from,
            FerroMetrics metrics) {
        this.mailSender = mailSender;
        this.from = from;
        this.metrics = metrics;
    }

    @Override
    public void sendVerificationEmail(Email recipient, String locale, String verificationLink) {
        if (english(locale)) {
            send(
                    recipient,
                    "verification",
                    "Verify your email for Ferro",
                    "Confirm your Ferro account by opening this link (valid 24 h):\n\n" + verificationLink);
        } else {
            send(
                    recipient,
                    "verification",
                    "Verifica tu email en Ferro",
                    "Confirma tu cuenta de Ferro abriendo este enlace (válido 24 h):\n\n" + verificationLink);
        }
    }

    @Override
    public void sendPasswordResetEmail(Email recipient, String locale, String resetLink) {
        if (english(locale)) {
            send(
                    recipient,
                    "password-reset",
                    "Reset your Ferro password",
                    "Reset your password by opening this link (valid 30 min):\n\n" + resetLink);
        } else {
            send(
                    recipient,
                    "password-reset",
                    "Recupera tu contraseña de Ferro",
                    "Restablece tu contraseña abriendo este enlace (válido 30 min):\n\n" + resetLink);
        }
    }

    @Override
    public void sendPasswordChangedEmail(Email recipient, String locale) {
        if (english(locale)) {
            send(
                    recipient,
                    "password-changed",
                    "Your Ferro password has changed",
                    "If you do not recognise this change, contact support.");
        } else {
            send(
                    recipient,
                    "password-changed",
                    "Tu contraseña de Ferro ha cambiado",
                    "Si no reconoces este cambio, contacta con soporte.");
        }
    }

    @Override
    public void sendAccountDeletedEmail(Email recipient, String locale) {
        if (english(locale)) {
            send(recipient, "account-deleted", "Your Ferro account has been deleted", "All your data has been erased.");
        } else {
            send(
                    recipient,
                    "account-deleted",
                    "Tu cuenta de Ferro ha sido eliminada",
                    "Todos tus datos han sido borrados.");
        }
    }

    @Override
    public void sendWelcomeEmail(Email recipient, String locale) {
        if (english(locale)) {
            send(
                    recipient,
                    "welcome",
                    "Welcome to Ferro",
                    "Your account is ready. Verify your email and start training.");
        } else {
            send(
                    recipient,
                    "welcome",
                    "Bienvenido a Ferro",
                    "Tu cuenta está creada. Verifica tu email y empieza a entrenar.");
        }
    }

    @Override
    public void sendSecurityAlertEmail(Email recipient, String locale) {
        if (english(locale)) {
            send(
                    recipient,
                    "security",
                    "Suspicious activity on your Ferro account",
                    "A revoked refresh token was reused. We closed all your sessions.");
        } else {
            send(
                    recipient,
                    "security",
                    "Actividad sospechosa en tu cuenta de Ferro",
                    "Se ha reutilizado un refresh token revocado. Hemos cerrado todas tus sesiones.");
        }
    }

    @Override
    public void sendSessionAutoClosed(String recipientEmail, String locale) {
        if (english(locale)) {
            send(
                    new Email(recipientEmail),
                    "auto-closed",
                    "Your Ferro session was closed automatically",
                    "More than 24 hours have passed since you started it. You can review it in history.");
        } else {
            send(
                    new Email(recipientEmail),
                    "auto-closed",
                    "Tu sesión de Ferro se ha cerrado automáticamente",
                    "Han pasado más de 24 horas desde que la iniciaste. Puedes revisarla en el historial.");
        }
    }

    @Override
    public void sendWeeklySummary(
            String recipientEmail,
            String locale,
            int sessions,
            java.math.BigDecimal volume,
            int personalRecords,
            int streak) {
        if (english(locale)) {
            send(
                    new Email(recipientEmail),
                    "weekly-summary",
                    "Your Ferro week in review",
                    "Sessions: " + sessions + "\nVolume: " + volume + " kg\nPRs: " + personalRecords
                            + "\nCurrent streak: " + streak + " days");
        } else {
            send(
                    new Email(recipientEmail),
                    "weekly-summary",
                    "Tu semana en Ferro",
                    "Sesiones: " + sessions + "\nVolumen: " + volume + " kg\nPRs: " + personalRecords
                            + "\nRacha actual: " + streak + " días");
        }
    }

    private boolean english(String locale) {
        return locale != null && locale.toLowerCase(java.util.Locale.ROOT).startsWith("en");
    }

    private void send(Email recipient, String template, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient.value());
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            metrics.emailSent(template);
        } catch (MailException exception) {
            // No bloqueamos la operación si Mailpit/SMTP no está disponible.
            log.warn("No se pudo enviar el email '{}': {}", subject, exception.getMessage());
        }
    }
}
