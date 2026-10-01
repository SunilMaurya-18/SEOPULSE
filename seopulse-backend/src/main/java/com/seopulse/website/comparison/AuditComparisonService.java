package com.seopulse.website.comparison;

import com.seopulse.common.exception.InvalidStateException;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.project.service.ProjectAccessService;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.seo.model.IssueSnapshot;
import com.seopulse.website.seo.repository.SeoIssueRepository;
import com.seopulse.website.seo.rules.RuleCatalog;
import com.seopulse.website.seo.rules.RuleDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditComparisonService {

    static final int MAX_LISTED_CHANGES = 100;
    static final int MAX_FINGERPRINTS = 2000;
    static final int MAX_TREND_POINTS = 100;

    private final AuditRepository auditRepository;
    private final SeoIssueRepository seoIssueRepository;
    private final ProjectAccessService projectAccessService;
    private final RuleCatalog ruleCatalog;

    public ComparisonResponse compare(Long projectId, Long auditId, Long baselineId, Long userId) {

        Audit target = projectAccessService.requireOwnedAudit(projectId, auditId, userId);

        if (target.getStatus() != AuditStatus.COMPLETED) {
            throw new InvalidStateException("Only completed audits can be compared");
        }

        Audit baseline;
        if (baselineId != null) {
            baseline = projectAccessService.requireOwnedAudit(projectId, baselineId, userId);
            if (!baseline.getWebsite().getId().equals(target.getWebsite().getId())) {
                throw new IllegalArgumentException("The baseline must be an audit of the same website");
            }
            if (baseline.getStatus() != AuditStatus.COMPLETED) {
                throw new InvalidStateException("The baseline audit has not completed");
            }
        } else {
            baseline = previousCompleted(auditId);
        }

        return toResponse(compare(target, baseline));
    }

    /** Compares with the previous completed audit; no access checks. */
    public AuditComparison compareWithPrevious(Long auditId) {
        Audit target = auditRepository.findByIdWithWebsite(auditId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit not found"));
        return compare(target, previousCompleted(auditId));
    }

    public List<TrendPoint> trend(Long projectId, Long websiteId, int limit, Long userId) {
        projectAccessService.requireOwnedWebsite(projectId, websiteId, userId);
        int size = Math.max(1, Math.min(limit, MAX_TREND_POINTS));
        List<Audit> audits = new ArrayList<>(auditRepository.findCompletedByWebsite(websiteId, PageRequest.of(0, size)));
        java.util.Collections.reverse(audits);
        return audits.stream().map(TrendPoint::from).toList();
    }

    AuditComparison compare(Audit target, Audit baseline) {

        if (baseline == null) {
            return new AuditComparison(target, null, target.getDetailsPurgedAt() == null,
                    List.of(), List.of(), 0, Map.of(), Map.of());
        }

        if (target.getDetailsPurgedAt() != null || baseline.getDetailsPurgedAt() != null) {
            return new AuditComparison(target, baseline, false, List.of(), List.of(), 0, Map.of(), Map.of());
        }

        Map<String, IssueSnapshot> current = byFingerprint(seoIssueRepository.findSnapshotsByAuditId(target.getId()));
        Map<String, IssueSnapshot> previous = byFingerprint(seoIssueRepository.findSnapshotsByAuditId(baseline.getId()));

        List<IssueSnapshot> added = current.entrySet().stream()
                .filter(entry -> !previous.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .sorted(BY_SEVERITY)
                .toList();

        List<IssueSnapshot> fixed = previous.entrySet().stream()
                .filter(entry -> !current.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .sorted(BY_SEVERITY)
                .toList();

        int persisting = (int) current.keySet().stream().filter(previous::containsKey).count();

        return new AuditComparison(target, baseline, true, added, fixed, persisting,
                countBySeverity(added), countBySeverity(fixed));
    }

    private Audit previousCompleted(Long auditId) {
        return auditRepository.findPreviousCompleted(auditId, PageRequest.of(0, 1)).stream()
                .findFirst()
                .orElse(null);
    }

    private ComparisonResponse toResponse(AuditComparison comparison) {
        Audit target = comparison.target();
        Audit baseline = comparison.baseline();
        return new ComparisonResponse(
                target.getId(),
                baseline == null ? null : baseline.getId(),
                baseline == null ? null : baseline.getCompletedAt(),
                target.getScore(),
                baseline == null ? null : baseline.getScore(),
                comparison.scoreDelta(),
                target.getPagesCrawled(),
                baseline == null ? null : baseline.getPagesCrawled(),
                baseline == null ? null : target.getPagesCrawled() - baseline.getPagesCrawled(),
                comparison.detailsAvailable(),
                comparison.newIssues().size(),
                comparison.fixedIssues().size(),
                comparison.persistingCount(),
                comparison.newBySeverity(),
                comparison.fixedBySeverity(),
                comparison.newIssues().stream().limit(MAX_LISTED_CHANGES).map(this::toChange).toList(),
                comparison.fixedIssues().stream().limit(MAX_LISTED_CHANGES).map(this::toChange).toList(),
                comparison.newIssues().stream().map(IssueSnapshot::fingerprint).limit(MAX_FINGERPRINTS).toList()
        );
    }

    private ComparisonResponse.IssueChange toChange(IssueSnapshot snapshot) {
        RuleDefinition rule = ruleCatalog.get(snapshot.ruleCode(), snapshot.severity());
        return new ComparisonResponse.IssueChange(
                snapshot.fingerprint(),
                snapshot.ruleCode(),
                rule.title(),
                snapshot.severity(),
                snapshot.category() != null ? snapshot.category() : rule.category().name(),
                snapshot.url(),
                snapshot.message()
        );
    }

    private static Map<String, IssueSnapshot> byFingerprint(List<IssueSnapshot> snapshots) {
        return snapshots.stream()
                .filter(snapshot -> snapshot.fingerprint() != null)
                .collect(Collectors.toMap(IssueSnapshot::fingerprint, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private static Map<String, Integer> countBySeverity(List<IssueSnapshot> issues) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String severity : List.of("ERROR", "WARNING", "INFO")) {
            counts.put(severity, 0);
        }
        for (IssueSnapshot issue : issues) {
            String severity = Objects.requireNonNullElse(issue.severity(), "INFO").toUpperCase();
            counts.merge(severity, 1, Integer::sum);
        }
        return counts;
    }

    private static int severityRank(String severity) {
        if (severity == null) {
            return 3;
        }
        return switch (severity.toUpperCase()) {
            case "ERROR" -> 0;
            case "WARNING" -> 1;
            case "INFO" -> 2;
            default -> 3;
        };
    }

    private static final Comparator<IssueSnapshot> BY_SEVERITY = Comparator
            .comparingInt((IssueSnapshot issue) -> severityRank(issue.severity()))
            .thenComparing(IssueSnapshot::ruleCode, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(IssueSnapshot::url, Comparator.nullsLast(Comparator.naturalOrder()));
}
