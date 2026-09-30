package com.seopulse.notification;

import com.seopulse.common.email.EmailMessage;
import com.seopulse.common.email.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;

public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailSender(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            boolean hasHtml = message.html() != null && !message.html().isBlank();
            MimeMessageHelper helper = new MimeMessageHelper(mime, hasHtml, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            if (hasHtml) {
                helper.setText(message.body(), message.html());
            } else {
                helper.setText(message.body());
            }
            mailSender.send(mime);
        } catch (MessagingException | MailException ex) {
            throw new IllegalStateException("SMTP delivery failed: " + ex.getMessage(), ex);
        }
    }
}
