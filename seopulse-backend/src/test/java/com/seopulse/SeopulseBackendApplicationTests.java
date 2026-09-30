package com.seopulse;

import com.seopulse.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against schema drift: Flyway must build the whole schema on an
 * empty database, and Hibernate must accept it with ddl-auto=validate.
 */
class SeopulseBackendApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationsBuildSchemaThatMatchesEntities() {

        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                """,
                String.class
        );

        List<String> appliedVersions = jdbcTemplate.queryForList(
                """
                SELECT version
                FROM flyway_schema_history
                WHERE success AND version IS NOT NULL
                ORDER BY installed_rank
                """,
                String.class
        );

        assertThat(appliedVersions).containsExactly("1", "2", "3", "4", "5", "6", "7");

        assertThat(tables).contains(
                "users",
                "projects",
                "websites",
                "audits",
                "audit_outbox",
                "audit_pages",
                "seo_issues",
                "refresh_tokens",
                "email_verification_tokens",
                "password_reset_tokens",
                "organizations",
                "organization_members",
                "invitations",
                "audit_log",
                "plans",
                "subscriptions",
                "stripe_events",
                "usage_counters",
                "email_outbox"
        );
    }
}
