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

/** Envío de emails transaccionales; en dev apunta a Mailpit (localhost:1025). */
@Component
public class MailEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(MailEmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;

    public MailEmailSender(JavaMailSender mailSender, @Value("${ferro.mail.from:no-reply@ferro.dev}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendVerificationEmail(Email recipient, String verificationLink) {
        send(
                recipient,
                "Verifica tu email en Ferro",
                "Confirma tu cuenta de Ferro abriendo este enlace (válido 24 h):\n\n" + verificationLink);
    }

    @Override
    public void sendPasswordResetEmail(Email recipient, String resetLink) {
        send(
                recipient,
                "Recupera tu contraseña de Ferro",
                "Restablece tu contraseña abriendo este enlace (válido 30 min):\n\n" + resetLink);
    }

    @Override
    public void sendPasswordChangedEmail(Email recipient) {
        send(recipient, "Tu contraseña de Ferro ha cambiado", "Si no reconoces este cambio, contacta con soporte.");
    }

    @Override
    public void sendAccountDeletedEmail(Email recipient) {
        send(recipient, "Tu cuenta de Ferro ha sido eliminada", "Todos tus datos han sido borrados.");
    }

    private void send(Email recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient.value());
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            // No bloqueamos la operación si Mailpit/SMTP no está disponible.
            log.warn("No se pudo enviar el email '{}': {}", subject, exception.getMessage());
        }
    }
}
