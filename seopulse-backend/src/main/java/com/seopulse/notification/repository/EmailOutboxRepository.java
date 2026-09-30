package com.seopulse.notification.repository;

import com.seopulse.notification.entity.EmailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    /** Web and worker processes both dispatch; row locks keep a message from being sent twice. */
    @Query(value = """
            SELECT * FROM email_outbox
             WHERE sent = false AND attempts < 5
             ORDER BY created_at
             LIMIT :limit
             FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<EmailOutbox> lockDispatchable(@Param("limit") int limit);
}
