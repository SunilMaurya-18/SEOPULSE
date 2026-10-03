package com.seopulse.admin;

import com.seopulse.billing.EntitlementService;
import com.seopulse.common.dto.PageResponse;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.user.entity.Role;
import com.seopulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Read-only platform views for operators. Every call re-checks the ADMIN role in the database. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NamedParameterJdbcTemplate jdbc;
    private final UserRepository userRepository;

    public void requireAdmin(Long userId) {
        boolean admin = userRepository.findById(userId).map(user -> user.getRole() == Role.ADMIN).orElse(false);
        if (!admin) {
            throw new AccessDeniedException("Admins only");
        }
    }

    public AdminDtos.Stats stats() {
        Instant now = Instant.now();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("d1", Timestamp.from(now.minus(Duration.ofDays(1))))
                .addValue("d7", Timestamp.from(now.minus(Duration.ofDays(7))))
                .addValue("d30", Timestamp.from(now.minus(Duration.ofDays(30))));

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT
                  (SELECT COUNT(*) FROM users) AS users,
                  (SELECT COUNT(*) FROM users WHERE created_at >= :d7) AS users7,
                  (SELECT COUNT(*) FROM users WHERE created_at >= :d30) AS users30,
                  (SELECT COUNT(*) FROM users WHERE email_verified_at IS NOT NULL) AS verified,
                  (SELECT COUNT(*) FROM organizations) AS orgs,
                  (SELECT COUNT(*) FROM websites) AS websites,
                  (SELECT COUNT(*) FROM audits) AS audits,
                  (SELECT COUNT(*) FROM audits WHERE created_at >= :d1) AS audits24,
                  (SELECT COUNT(*) FROM audits WHERE status = 'FAILED' AND completed_at >= :d1) AS failed24,
                  (SELECT COUNT(*) FROM audits WHERE status IN ('QUEUED', 'CRAWLING', 'ANALYZING')) AS active,
                  (SELECT COUNT(*) FROM newsletter_subscribers
                     WHERE confirmed_at IS NOT NULL AND unsubscribed_at IS NULL) AS subscribers
                """, params);

        Map<String, Long> byPlan = new LinkedHashMap<>();
        jdbc.query("""
                SELECT p.code, COUNT(*) AS n
                FROM subscriptions s JOIN plans p ON p.id = s.plan_id
                WHERE s.status <> 'CANCELED'
                GROUP BY p.code
                ORDER BY n DESC
                """, rs -> {
            byPlan.put(rs.getString("code"), rs.getLong("n"));
        });

        return new AdminDtos.Stats(
                count(row, "users"), count(row, "users7"), count(row, "users30"), count(row, "verified"),
                count(row, "orgs"), count(row, "websites"), count(row, "audits"), count(row, "audits24"),
                count(row, "failed24"), count(row, "active"), count(row, "subscribers"), byPlan);
    }

    public PageResponse<AdminDtos.UserRow> users(String query, int page, int size) {
        MapSqlParameterSource params = search(query);
        String where = " WHERE (:q IS NULL OR LOWER(u.email) LIKE :q OR LOWER(u.name) LIKE :q)";
        RowMapper<AdminDtos.UserRow> mapper = (rs, i) -> new AdminDtos.UserRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("role"),
                rs.getTimestamp("email_verified_at") != null,
                rs.getString("google_subject") != null,
                rs.getTimestamp("locked_until") != null && rs.getTimestamp("locked_until").toInstant().isAfter(Instant.now()),
                rs.getLong("workspaces"),
                rs.getTimestamp("created_at").toInstant());
        return paged("""
                SELECT u.id, u.name, u.email, u.role, u.email_verified_at, u.google_subject, u.locked_until, u.created_at,
                       (SELECT COUNT(*) FROM organization_members m WHERE m.user_id = u.id) AS workspaces
                FROM users u""" + where + " ORDER BY u.created_at DESC, u.id DESC",
                "SELECT COUNT(*) FROM users u" + where, params, mapper, page, size);
    }

    public PageResponse<AdminDtos.OrganizationRow> organizations(String query, int page, int size) {
        MapSqlParameterSource params = search(query)
                .addValue("periodStart", LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1))
                .addValue("meter", EntitlementService.AUDITS);
        String where = " WHERE (:q IS NULL OR LOWER(o.name) LIKE :q OR LOWER(o.slug) LIKE :q)";
        RowMapper<AdminDtos.OrganizationRow> mapper = (rs, i) -> new AdminDtos.OrganizationRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("plan"),
                rs.getString("status"),
                rs.getLong("members"),
                rs.getLong("websites"),
                rs.getLong("audits_month"),
                rs.getTimestamp("created_at").toInstant());
        return paged("""
                SELECT o.id, o.name, o.created_at, p.code AS plan, s.status,
                       (SELECT COUNT(*) FROM organization_members m WHERE m.organization_id = o.id) AS members,
                       (SELECT COUNT(*) FROM websites w JOIN projects pr ON pr.id = w.project_id
                          WHERE pr.organization_id = o.id) AS websites,
                       COALESCE((SELECT SUM(c.used) FROM usage_counters c
                          WHERE c.organization_id = o.id AND c.meter = :meter AND c.period_start = :periodStart), 0)
                          AS audits_month
                FROM organizations o
                LEFT JOIN subscriptions s ON s.organization_id = o.id
                LEFT JOIN plans p ON p.id = s.plan_id""" + where + " ORDER BY o.created_at DESC, o.id DESC",
                "SELECT COUNT(*) FROM organizations o" + where, params, mapper, page, size);
    }

    public PageResponse<AdminDtos.FailedAuditRow> failedAudits(int page, int size) {
        RowMapper<AdminDtos.FailedAuditRow> mapper = (rs, i) -> new AdminDtos.FailedAuditRow(
                rs.getLong("id"),
                rs.getString("url"),
                rs.getObject("org_id") == null ? null : rs.getLong("org_id"),
                rs.getString("org_name"),
                rs.getString("error_message"),
                rs.getInt("retry_count"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant());
        return paged("""
                SELECT a.id, w.url, o.id AS org_id, o.name AS org_name, a.error_message, a.retry_count,
                       a.created_at, a.completed_at
                FROM audits a
                JOIN websites w ON w.id = a.website_id
                JOIN projects pr ON pr.id = w.project_id
                LEFT JOIN organizations o ON o.id = pr.organization_id
                WHERE a.status = 'FAILED'
                ORDER BY a.completed_at DESC NULLS LAST, a.id DESC""",
                "SELECT COUNT(*) FROM audits a WHERE a.status = 'FAILED'",
                new MapSqlParameterSource(), mapper, page, size);
    }

    public void unlockUser(Long targetUserId, Long adminId) {
        if (!userRepository.existsById(targetUserId)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.resetLoginFailures(targetUserId);
        log.info("Admin unlocked account: adminId={}, userId={}", adminId, targetUserId);
    }

    private <T> PageResponse<T> paged(
            String sql, String countSql, MapSqlParameterSource params, RowMapper<T> mapper, int page, int size
    ) {
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        int safePage = Math.max(0, page);
        long total = jdbc.queryForObject(countSql, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) safePage * safeSize);
        List<T> rows = jdbc.query(sql + " LIMIT :limit OFFSET :offset", params, mapper);
        int totalPages = (int) Math.ceil(total / (double) safeSize);
        return new PageResponse<>(rows, safePage, safeSize, total, totalPages, safePage == 0, safePage + 1 >= totalPages);
    }

    private static MapSqlParameterSource search(String query) {
        String q = query == null || query.isBlank()
                ? null
                : "%" + query.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return new MapSqlParameterSource().addValue("q", q, java.sql.Types.VARCHAR);
    }

    private static long count(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.longValue() : 0;
    }
}
