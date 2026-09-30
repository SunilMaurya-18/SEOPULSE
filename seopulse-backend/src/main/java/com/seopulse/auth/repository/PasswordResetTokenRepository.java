package com.seopulse.auth.repository;

import com.seopulse.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Query("SELECT t FROM PasswordResetToken t JOIN FETCH t.user WHERE t.tokenHash = :hash")
    Optional<PasswordResetToken> findByTokenHash(@Param("hash") String hash);

    @Modifying
    @Query("""
            UPDATE PasswordResetToken t SET t.usedAt = :now
            WHERE t.user.id = :userId AND t.usedAt IS NULL
            """)
    int invalidateAllForUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
