package com.seopulse.admin;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "seopulse.admin")
@Getter
@Setter
public class AdminProperties {

    /** Accounts with these emails are made platform admins on startup and at sign-in. */
    private List<String> emails = new ArrayList<>();

    public Set<String> normalizedEmails() {
        return emails.stream()
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
