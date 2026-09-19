package com.minutemart.quickcommerce.identity.infrastructure.persistence;

import com.minutemart.quickcommerce.identity.application.AuthSessionRepository;
import com.minutemart.quickcommerce.identity.domain.AuthSession;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAuthSessionRepository implements AuthSessionRepository {

    private final SpringDataAuthSessionRepository delegate;

    public JpaAuthSessionRepository(SpringDataAuthSessionRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<AuthSession> findByAccessTokenHash(String accessTokenHash) {
        return delegate.findByAccessTokenHash(accessTokenHash).map(AuthSessionEntity::toDomain);
    }

    @Override
    public Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash) {
        return delegate.findByRefreshTokenHash(refreshTokenHash).map(AuthSessionEntity::toDomain);
    }

    @Override
    public Optional<AuthSession> findByRefreshTokenHashForUpdate(String refreshTokenHash) {
        return delegate.findByRefreshTokenHashForUpdate(refreshTokenHash).map(AuthSessionEntity::toDomain);
    }

    @Override
    public int consumeRefreshToken(UUID sessionId, Instant revokedAt, Instant now) {
        return delegate.consumeRefreshToken(sessionId, revokedAt, now);
    }

    @Override
    public Optional<AuthSession> findById(UUID sessionId) {
        return delegate.findById(sessionId).map(AuthSessionEntity::toDomain);
    }

    @Override
    public AuthSession save(AuthSession session) {
        AuthSessionEntity entity = delegate.findById(session.sessionId())
                .map(existing -> {
                    existing.apply(session);
                    return existing;
                })
                .orElseGet(() -> AuthSessionEntity.fromDomain(session));
        return delegate.save(entity).toDomain();
    }

    @Override
    public void revokeAllForIdentity(UUID identityId, Instant revokedAt) {
        delegate.revokeAllForIdentity(identityId, revokedAt);
    }

    @Override
    public void revokeForIdentityAndRole(
            UUID identityId,
            com.minutemart.quickcommerce.identity.api.IdentityRole role,
            com.minutemart.quickcommerce.identity.api.ScopeType scopeType,
            String scopeId,
            Instant revokedAt
    ) {
        delegate.revokeForIdentityAndRole(identityId, role, scopeType,
                scopeId == null ? "" : scopeId, revokedAt);
    }
}
