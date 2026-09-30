package com.seopulse.support;

import com.seopulse.common.email.EmailMessage;
import com.seopulse.common.email.EmailSender;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Records outgoing emails so tests can follow verification and reset links.
 * Emails are queued in the outbox, so lookups flush it first.
 */
public class CapturingEmailSender implements EmailSender {

    private static final Pattern TOKEN = Pattern.compile("[?&]token=([A-Za-z0-9_\\-]+)");

    private final List<EmailMessage> sent = new CopyOnWriteArrayList<>();
    private final Runnable flushOutbox;

    public CapturingEmailSender(Runnable flushOutbox) {
        this.flushOutbox = flushOutbox;
    }

    @Override
    public void send(EmailMessage message) {
        sent.add(message);
    }

    public List<EmailMessage> sentTo(String email) {
        flushOutbox.run();
        return sent.stream().filter(message -> message.to().equals(email)).toList();
    }

    public Optional<EmailMessage> lastTo(String email, String subjectFragment) {
        List<EmailMessage> matching = sentTo(email).stream()
                .filter(message -> message.subject().contains(subjectFragment))
                .toList();
        return matching.isEmpty() ? Optional.empty() : Optional.of(matching.getLast());
    }

    public String lastTokenTo(String email, String subjectFragment) {
        EmailMessage message = lastTo(email, subjectFragment)
                .orElseThrow(() -> new AssertionError("No email '" + subjectFragment + "' sent to " + email));
        Matcher matcher = TOKEN.matcher(message.body());
        if (!matcher.find()) {
            throw new AssertionError("Email has no token link: " + message.body());
        }
        return matcher.group(1);
    }
}
