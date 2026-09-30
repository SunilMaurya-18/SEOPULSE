package com.seopulse.notification;

import com.seopulse.notification.entity.EmailOutbox;
import com.seopulse.notification.repository.EmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailOutboxService {

    private final EmailOutboxRepository emailOutboxRepository;

    @Transactional
    public void enqueue(String to, String subject, String text, String html) {
        emailOutboxRepository.save(EmailOutbox.builder()
                .toAddress(to)
                .subject(subject.length() > 200 ? subject.substring(0, 200) : subject)
                .bodyText(text)
                .bodyHtml(html)
                .sent(false)
                .attempts(0)
                .build());
    }
}
