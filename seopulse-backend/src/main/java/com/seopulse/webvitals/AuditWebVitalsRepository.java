package com.seopulse.webvitals;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditWebVitalsRepository extends JpaRepository<AuditWebVitals, Long> {
}
