package com.minutemart.quickcommerce.identity.application;

import com.minutemart.quickcommerce.identity.domain.AuthSession;

import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository {

    Optional<AuthSession> findByAccessTokenHash(String accessTokenHash);

    Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);

    Optional<AuthSession> findByRefreshTokenHashForUpdate(String refreshTokenHash);

    int consumeRefreshToken(UUID sessionId, java.time.Instant revokedAt, java.time.Instant now);

    Optional<AuthSession> findById(UUID sessionId);

    AuthSession save(AuthSession session);

    void revokeAllForIdentity(UUID identityId, java.time.Instant revokedAt);

    void revokeForIdentityAndRole(
            UUID identityId,
            com.minutemart.quickcommerce.identity.api.IdentityRole role,
            com.minutemart.quickcommerce.identity.api.ScopeType scopeType,
            String scopeId,
            java.time.Instant revokedAt
    );
}
