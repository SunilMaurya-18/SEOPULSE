package com.seopulse.admin;

import com.seopulse.user.entity.Role;
import com.seopulse.user.entity.User;
import com.seopulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Grants the platform ADMIN role to the accounts listed in SEOPULSE_ADMIN_EMAILS. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap {

    private final AdminProperties properties;
    private final UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void promoteListedAccounts() {
        Set<String> emails = properties.normalizedEmails();
        if (emails.isEmpty()) {
            return;
        }
        int promoted = userRepository.promoteToAdmin(emails);
        if (promoted > 0) {
            log.info("Granted platform admin to {} listed account(s)", promoted);
        }
    }

    /** Covers listed accounts created after startup. Returns the (possibly updated) user. */
    public User promoteIfListed(User user) {
        if (user.getRole() != Role.ADMIN && properties.normalizedEmails().contains(user.getEmail())) {
            user.setRole(Role.ADMIN);
            log.info("Granted platform admin: userId={}", user.getId());
            return userRepository.save(user);
        }
        return user;
    }
}
