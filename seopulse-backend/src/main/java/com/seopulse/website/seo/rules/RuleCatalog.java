package com.seopulse.website.seo.rules;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rule metadata (category, weight, recommendation, help link) from the
 * {@code seo_rules} table, cached briefly so analysis does not query it
 * per issue while edits still take effect without a restart.
 */
@Service
@RequiredArgsConstructor
public class RuleCatalog {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final SeoRuleRepository repository;

    private volatile Map<String, RuleDefinition> rules;
    private volatile Instant loadedAt = Instant.EPOCH;

    public RuleDefinition get(String code, String severity) {
        RuleDefinition rule = rules().get(code);
        return rule != null ? rule : RuleDefinition.fallback(code, severity);
    }

    public Collection<RuleDefinition> all() {
        return rules().values();
    }

    private Map<String, RuleDefinition> rules() {
        Map<String, RuleDefinition> current = rules;
        if (current == null || loadedAt.plus(TTL).isBefore(Instant.now())) {
            current = repository.findAll().stream()
                    .map(RuleDefinition::from)
                    .collect(Collectors.toUnmodifiableMap(RuleDefinition::code, Function.identity()));
            rules = current;
            loadedAt = Instant.now();
        }
        return current;
    }
}
