package com.ferro.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, UUID> {

    Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update PasswordResetTokenJpaEntity t set t.usedAt = :now "
            + "where t.userId = :userId and t.usedAt is null")
    int invalidateActiveForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
