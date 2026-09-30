package com.seopulse.organization.repository;

import com.seopulse.organization.entity.OrgAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgAuditLogRepository extends JpaRepository<OrgAuditLog, Long> {
}
