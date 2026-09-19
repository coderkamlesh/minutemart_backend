package com.minutemart.quickcommerce.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface SpringDataAuthSessionRepository extends JpaRepository<AuthSessionEntity, UUID> {

    Optional<AuthSessionEntity> findByAccessTokenHash(String accessTokenHash);

    Optional<AuthSessionEntity> findByRefreshTokenHash(String refreshTokenHash);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from AuthSessionEntity session where session.refreshTokenHash = :refreshTokenHash")
    Optional<AuthSessionEntity> findByRefreshTokenHashForUpdate(@Param("refreshTokenHash") String refreshTokenHash);

    @Modifying
    @Query("update AuthSessionEntity session set session.revokedAt = :revokedAt "
            + "where session.sessionId = :sessionId and session.revokedAt is null "
            + "and session.refreshExpiresAt > :now")
    int consumeRefreshToken(
            @Param("sessionId") UUID sessionId,
            @Param("revokedAt") Instant revokedAt,
            @Param("now") Instant now
    );

    @Modifying
    @Query("update AuthSessionEntity session set session.revokedAt = :revokedAt "
            + "where session.identityId = :identityId and session.revokedAt is null")
    int revokeAllForIdentity(@Param("identityId") UUID identityId, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("update AuthSessionEntity session set session.revokedAt = :revokedAt "
            + "where session.identityId = :identityId and session.role = :role "
            + "and session.scopeType = :scopeType and session.scopeId = :scopeId "
            + "and session.revokedAt is null")
    int revokeForIdentityAndRole(
            @Param("identityId") UUID identityId,
            @Param("role") com.minutemart.quickcommerce.identity.api.IdentityRole role,
            @Param("scopeType") com.minutemart.quickcommerce.identity.api.ScopeType scopeType,
            @Param("scopeId") String scopeId,
            @Param("revokedAt") Instant revokedAt
    );
}
