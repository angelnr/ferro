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

/** Envío de emails de verificación; en dev apunta a Mailpit (localhost:1025). */
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient.value());
        message.setSubject("Verifica tu email en Ferro");
        message.setText("""
                Hola:

                Confirma tu cuenta de Ferro abriendo este enlace (válido 24 h):

                %s

                Si no has creado la cuenta, ignora este mensaje.
                """.formatted(verificationLink));
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            // No bloqueamos el registro si Mailpit/SMTP no está disponible: el usuario puede reenviar.
            log.warn("No se pudo enviar el email de verificación: {}", exception.getMessage());
        }
    }
}
