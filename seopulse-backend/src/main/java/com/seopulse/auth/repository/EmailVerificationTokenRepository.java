package com.seopulse.auth.repository;

import com.seopulse.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    @Query("SELECT t FROM EmailVerificationToken t JOIN FETCH t.user WHERE t.tokenHash = :hash")
    Optional<EmailVerificationToken> findByTokenHash(@Param("hash") String hash);

    @Modifying
    @Query("""
            UPDATE EmailVerificationToken t SET t.usedAt = :now
            WHERE t.user.id = :userId AND t.usedAt IS NULL
            """)
    int invalidateAllForUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
