package com.seopulse.schedule;

import com.seopulse.billing.EntitlementService;
import com.seopulse.billing.PlanLimits;
import com.seopulse.organization.entity.OrganizationRole;
import com.seopulse.organization.service.OrganizationAccessService;
import com.seopulse.project.service.ProjectAccessService;
import com.seopulse.website.entity.Website;
import com.seopulse.website.service.AuditService;
import com.seopulse.website.service.AuditService.ScheduledAuditResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Service
@Slf4j
public class ScheduleService {

    static final int MAX_DISPATCH_PER_RUN = 200;

    private final AuditScheduleRepository scheduleRepository;
    private final ProjectAccessService projectAccessService;
    private final OrganizationAccessService organizationAccessService;
    private final EntitlementService entitlementService;
    private final AuditService auditService;
    private final TransactionTemplate claimTransaction;
    private final TransactionTemplate auditTransaction;

    public ScheduleService(
            AuditScheduleRepository scheduleRepository,
            ProjectAccessService projectAccessService,
            OrganizationAccessService organizationAccessService,
            EntitlementService entitlementService,
            AuditService auditService,
            PlatformTransactionManager transactionManager
    ) {
        this.scheduleRepository = scheduleRepository;
        this.projectAccessService = projectAccessService;
        this.organizationAccessService = organizationAccessService;
        this.entitlementService = entitlementService;
        this.auditService = auditService;
        this.claimTransaction = new TransactionTemplate(transactionManager);
        this.auditTransaction = new TransactionTemplate(transactionManager);
        this.auditTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(readOnly = true)
    public ScheduleResponse get(Long projectId, Long websiteId, Long userId) {
        Website website = projectAccessService.requireOwnedWebsite(projectId, websiteId, userId);
        String allowed = entitlementService.limitsFor(organizationId(website)).schedule();
        return scheduleRepository.findByWebsiteId(websiteId)
                .map(schedule -> ScheduleResponse.from(schedule, allowed))
                .orElseGet(() -> ScheduleResponse.none(websiteId, allowed));
    }

    @Transactional
    public ScheduleResponse upsert(Long projectId, Long websiteId, Long userId, ScheduleRequest request) {

        Website website = projectAccessService.requireOwnedWebsite(projectId, websiteId, userId);
        Long organizationId = organizationId(website);
        organizationAccessService.requireRole(organizationId, userId, OrganizationRole.MEMBER);

        boolean enabled = request.enabled() == null || request.enabled();
        if (enabled) {
            entitlementService.requireSchedule(organizationId, request.frequency().name());
        }

        ZoneId zone = parseZone(request.timezone());
        Short dayOfWeek = request.frequency() == ScheduleFrequency.WEEKLY
                ? (short) (request.dayOfWeek() == null ? 1 : request.dayOfWeek())
                : null;

        AuditSchedule schedule = scheduleRepository.findByWebsiteId(websiteId)
                .orElseGet(() -> AuditSchedule.builder().website(website).createdBy(userId).build());

        schedule.setFrequency(request.frequency());
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setHourOfDay((short) request.hourOfDay());
        schedule.setTimezone(zone.getId());
        schedule.setEnabled(enabled);
        schedule.setLastError(null);
        schedule.setNextRunAt(enabled
                ? ScheduleCalculator.withJitter(nextSlot(schedule, Instant.now()))
                : null);

        AuditSchedule saved = scheduleRepository.save(schedule);
        log.info("Audit schedule saved: websiteId={}, frequency={}, enabled={}", websiteId, request.frequency(), enabled);
        return ScheduleResponse.from(saved, entitlementService.limitsFor(organizationId).schedule());
    }

    @Transactional
    public void delete(Long projectId, Long websiteId, Long userId) {
        Website website = projectAccessService.requireOwnedWebsite(projectId, websiteId, userId);
        organizationAccessService.requireRole(organizationId(website), userId, OrganizationRole.MEMBER);
        scheduleRepository.findByWebsiteId(websiteId).ifPresent(scheduleRepository::delete);
    }

    /** Starts audits for every due schedule. Returns how many schedules were processed. */
    public int dispatchDue() {
        int processed = 0;
        while (processed < MAX_DISPATCH_PER_RUN) {
            Boolean claimed = claimTransaction.execute(status -> dispatchNext(Instant.now()));
            if (!Boolean.TRUE.equals(claimed)) {
                break;
            }
            processed++;
        }
        return processed;
    }

    /**
     * Claims one due schedule, starts its audit in a separate transaction and
     * always advances {@code next_run_at}, so a failing audit cannot block
     * the schedule or the rest of the queue.
     */
    private boolean dispatchNext(Instant now) {

        AuditSchedule schedule = scheduleRepository.lockNextDue(now).orElse(null);
        if (schedule == null) {
            return false;
        }

        Website website = schedule.getWebsite();
        Long organizationId = organizationId(website);
        String error;

        try {
            PlanLimits limits = entitlementService.limitsFor(organizationId);
            if (!limits.allowsSchedule(schedule.getFrequency().name())) {
                error = "Skipped: your plan does not include " + schedule.getFrequency().name().toLowerCase() + " audits";
            } else {
                ScheduledAuditResult result = auditTransaction.execute(status -> auditService.createScheduledAudit(website));
                error = switch (result.outcome()) {
                    case CREATED -> {
                        schedule.setLastAuditId(result.auditId());
                        yield null;
                    }
                    case ALREADY_RUNNING -> "Skipped: an audit was already running";
                    case QUOTA_EXHAUSTED -> "Skipped: the monthly audit quota is used up";
                    case WEBSITE_INACTIVE -> "Skipped: the website is locked";
                };
            }
        } catch (RuntimeException ex) {
            log.error("Scheduled audit failed to start: scheduleId={}, websiteId={}", schedule.getId(), website.getId(), ex);
            error = "Skipped: the audit could not be started";
        }

        schedule.setLastRunAt(now);
        schedule.setLastError(error);
        schedule.setNextRunAt(ScheduleCalculator.withJitter(nextSlot(schedule, now)));
        return true;
    }

    private static Instant nextSlot(AuditSchedule schedule, Instant after) {
        return ScheduleCalculator.nextRun(
                schedule.getFrequency(),
                schedule.getDayOfWeek() == null ? null : schedule.getDayOfWeek().intValue(),
                schedule.getHourOfDay(),
                parseZone(schedule.getTimezone()),
                after
        );
    }

    private static ZoneId parseZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Unknown time zone: " + timezone);
        }
    }

    private static Long organizationId(Website website) {
        return website.getProject().getOrganization().getId();
    }
}
