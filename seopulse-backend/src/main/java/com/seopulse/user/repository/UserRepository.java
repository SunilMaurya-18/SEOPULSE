package com.seopulse.user.repository;

import com.seopulse.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByGoogleSubject(String googleSubject);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.role = com.seopulse.user.entity.Role.ADMIN WHERE u.email IN :emails AND u.role <> com.seopulse.user.entity.Role.ADMIN")
    int promoteToAdmin(@Param("emails") Collection<String> emails);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.failedLoginCount = u.failedLoginCount + 1 WHERE u.id = :id")
    int incrementFailedLogins(@Param("id") Long id);

    /**
     * Locks the account and resets the counter once it reaches {@code max}.
     * Returns 1 only for the request that performed the lock.
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE User u SET u.lockedUntil = :until, u.failedLoginCount = 0
            WHERE u.id = :id AND u.failedLoginCount >= :max
            """)
    int lockIfThresholdReached(@Param("id") Long id, @Param("max") int max, @Param("until") Instant until);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.failedLoginCount = 0, u.lockedUntil = NULL WHERE u.id = :id")
    int resetLoginFailures(@Param("id") Long id);
}
