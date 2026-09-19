package com.minutemart.quickcommerce.identity.infrastructure.persistence;

import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.ClientApplication;
import com.minutemart.quickcommerce.identity.api.ScopeType;
import com.minutemart.quickcommerce.identity.domain.AuthSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identity_auth_session")
class AuthSessionEntity {

    @Id
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    @Column(name = "access_token_hash", nullable = false, unique = true, length = 128)
    private String accessTokenHash;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 128)
    private String refreshTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private IdentityRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private ScopeType scopeType;

    @Column(name = "scope_id", nullable = false, length = 255)
    private String scopeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_application", nullable = false, length = 32)
    private ClientApplication clientApplication;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "access_expires_at", nullable = false)
    private Instant accessExpiresAt;

    @Column(name = "refresh_expires_at", nullable = false)
    private Instant refreshExpiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    protected AuthSessionEntity() {
    }

    static AuthSessionEntity fromDomain(AuthSession session) {
        AuthSessionEntity entity = new AuthSessionEntity();
        entity.apply(session);
        return entity;
    }

    void apply(AuthSession session) {
        sessionId = session.sessionId();
        identityId = session.identityId();
        accessTokenHash = session.accessTokenHash();
        refreshTokenHash = session.refreshTokenHash();
        role = session.role();
        scopeType = session.scopeType();
        scopeId = session.scopeId() == null ? "" : session.scopeId();
        clientApplication = session.clientApplication();
        createdAt = session.createdAt();
        accessExpiresAt = session.accessExpiresAt();
        refreshExpiresAt = session.refreshExpiresAt();
        revokedAt = session.revokedAt();
        lastUsedAt = session.lastUsedAt();
    }

    AuthSession toDomain() {
        return AuthSession.rehydrate(
                sessionId,
                identityId,
                accessTokenHash,
                refreshTokenHash,
                role,
                scopeType,
                scopeId.isBlank() ? null : scopeId,
                clientApplication,
                createdAt,
                accessExpiresAt,
                refreshExpiresAt,
                revokedAt,
                lastUsedAt
        );
    }
}
