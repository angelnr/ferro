package com.ferro.identity.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.ferro.identity.domain.Email;

import jakarta.mail.internet.MimeMessage;

class MailEmailSenderTest {

    @Test
    void sends_english_and_spanish_templates() {
        CapturingMailSender mail = new CapturingMailSender();
        MailEmailSender sender = new MailEmailSender(mail, "no-reply@ferro.dev");

        sender.sendVerificationEmail(new Email("en@ferro.dev"), "en", "https://ferro.dev/v");
        sender.sendWelcomeEmail(new Email("es@ferro.dev"), "es");

        assertThat(mail.sent).hasSize(2);
        assertThat(mail.sent.get(0).getSubject()).isEqualTo("Verify your email for Ferro");
        assertThat(mail.sent.get(0).getText()).contains("https://ferro.dev/v");
        assertThat(mail.sent.get(1).getSubject()).isEqualTo("Bienvenido a Ferro");
    }

    static class CapturingMailSender implements JavaMailSender {
        final List<SimpleMailMessage> sent = new ArrayList<>();

        @Override
        public MimeMessage createMimeMessage() {
            throw new UnsupportedOperationException();
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(MimeMessage mimeMessage) throws MailException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(MimeMessage... mimeMessages) throws MailException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) throws MailException {
            sent.add(simpleMessage);
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            sent.addAll(List.of(simpleMessages));
        }
    }
}
