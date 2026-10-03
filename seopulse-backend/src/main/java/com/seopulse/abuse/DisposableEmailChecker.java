package com.seopulse.abuse;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class DisposableEmailChecker {

    private final AbuseProperties properties;
    private final Set<String> domains;

    public DisposableEmailChecker(AbuseProperties properties) {
        this.properties = properties;
        this.domains = load();
        properties.getExtraBlockedDomains().forEach(domain -> domains.add(domain.trim().toLowerCase(Locale.ROOT)));
    }

    /**
     * @throws IllegalArgumentException if the address belongs to a throwaway email provider
     */
    public void requireAllowed(String email) {
        if (isDisposable(email)) {
            throw new IllegalArgumentException("Please sign up with a permanent email address, not a temporary one.");
        }
    }

    public boolean isDisposable(String email) {
        if (!properties.isBlockDisposableEmails() || email == null) {
            return false;
        }
        int at = email.lastIndexOf('@');
        if (at < 0) {
            return false;
        }
        String domain = email.substring(at + 1).trim().toLowerCase(Locale.ROOT);
        while (!domain.isEmpty()) {
            if (domains.contains(domain)) {
                return true;
            }
            int dot = domain.indexOf('.');
            if (dot < 0) {
                return false;
            }
            domain = domain.substring(dot + 1);
        }
        return false;
    }

    private static Set<String> load() {
        try (InputStream in = new ClassPathResource("abuse/disposable-domains.txt").getInputStream()) {
            Set<String> result = new HashSet<>();
            new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.toLowerCase(Locale.ROOT))
                    .forEach(result::add);
            return result;
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not load the disposable email domain list", ex);
        }
    }
}
