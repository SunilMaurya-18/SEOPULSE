package com.seopulse.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seopulse.common.email.EmailSender;
import com.seopulse.common.email.LoggingEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@Slf4j
public class EmailConfig {

    @Bean
    EmailSender emailSender(
            @Value("${seopulse.email.resend-api-key:}") String apiKey,
            @Value("${seopulse.email.from:SEOPulse <noreply@seopulse.app>}") String from,
            @Value("${seopulse.email.log-content:false}") boolean logContent,
            @Value("${spring.mail.host:}") String smtpHost,
            ObjectProvider<JavaMailSender> mailSender,
            ObjectMapper objectMapper
    ) {
        if (apiKey != null && !apiKey.isBlank()) {
            log.info("Email delivery: Resend (from {})", from);
            return new ResendEmailSender(apiKey, from, objectMapper);
        }
        JavaMailSender smtp = mailSender.getIfAvailable();
        if (smtp != null && smtpHost != null && !smtpHost.isBlank()) {
            log.info("Email delivery: SMTP via {} (from {})", smtpHost, from);
            return new SmtpEmailSender(smtp, from);
        }
        log.warn("Email delivery: none configured, emails are only logged. Set SMTP_HOST or RESEND_API_KEY to send real mail.");
        return new LoggingEmailSender(logContent);
    }
}
