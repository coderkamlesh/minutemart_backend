package com.minutemart.quickcommerce.identity.domain;

import com.minutemart.quickcommerce.identity.api.ClientApplication;
import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.ScopeType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AuthSession {

    private final UUID sessionId;
    private final UUID identityId;
    private final String accessTokenHash;
    private final String refreshTokenHash;
    private final IdentityRole role;
    private final ScopeType scopeType;
    private final String scopeId;
    private final ClientApplication clientApplication;
    private final Instant createdAt;
    private final Instant accessExpiresAt;
    private final Instant refreshExpiresAt;
    private Instant revokedAt;
    private Instant lastUsedAt;

    private AuthSession(
            UUID sessionId,
            UUID identityId,
            String accessTokenHash,
            String refreshTokenHash,
            IdentityRole role,
            ScopeType scopeType,
            String scopeId,
            ClientApplication clientApplication,
            Instant createdAt,
            Instant accessExpiresAt,
            Instant refreshExpiresAt,
            Instant revokedAt,
            Instant lastUsedAt
    ) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.identityId = Objects.requireNonNull(identityId);
        this.accessTokenHash = Objects.requireNonNull(accessTokenHash);
        this.refreshTokenHash = Objects.requireNonNull(refreshTokenHash);
        this.role = Objects.requireNonNull(role);
        this.scopeType = Objects.requireNonNull(scopeType);
        this.scopeId = scopeId;
        this.clientApplication = Objects.requireNonNull(clientApplication);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.accessExpiresAt = Objects.requireNonNull(accessExpiresAt);
        this.refreshExpiresAt = Objects.requireNonNull(refreshExpiresAt);
        this.revokedAt = revokedAt;
        this.lastUsedAt = lastUsedAt;
        RoleScope.validate(role, scopeType, scopeId);
        if (!clientApplication.supports(role)) {
            throw new IdentityException(IdentityError.INVALID_CLIENT_APPLICATION);
        }
    }

    public static AuthSession issue(
            UUID sessionId,
            UUID identityId,
            String accessTokenHash,
            String refreshTokenHash,
            IdentityRole role,
            ScopeType scopeType,
            String scopeId,
            ClientApplication clientApplication,
            Instant createdAt,
            Instant accessExpiresAt,
            Instant refreshExpiresAt
    ) {
        if (clientApplication == null || !clientApplication.supports(role)) {
            throw new IdentityException(IdentityError.INVALID_CLIENT_APPLICATION);
        }
        if (!accessExpiresAt.isAfter(createdAt) || !refreshExpiresAt.isAfter(accessExpiresAt)) {
            throw new IllegalArgumentException("invalid session expiry policy");
        }
        return new AuthSession(sessionId, identityId, accessTokenHash, refreshTokenHash,
                role, scopeType, scopeId, clientApplication, createdAt, accessExpiresAt, refreshExpiresAt,
                null, null);
    }

    public static AuthSession rehydrate(
            UUID sessionId,
            UUID identityId,
            String accessTokenHash,
            String refreshTokenHash,
            IdentityRole role,
            ScopeType scopeType,
            String scopeId,
            ClientApplication clientApplication,
            Instant createdAt,
            Instant accessExpiresAt,
            Instant refreshExpiresAt,
            Instant revokedAt,
            Instant lastUsedAt
    ) {
        return new AuthSession(sessionId, identityId, accessTokenHash, refreshTokenHash,
                role, scopeType, scopeId, clientApplication, createdAt, accessExpiresAt, refreshExpiresAt,
                revokedAt, lastUsedAt);
    }

    public void authenticate(Instant now) {
        if (revokedAt != null || !accessExpiresAt.isAfter(now)) {
            throw new IdentityException(IdentityError.SESSION_INVALID);
        }
        lastUsedAt = now;
    }

    public void ensureRefreshable(Instant now) {
        if (revokedAt != null || !refreshExpiresAt.isAfter(now)) {
            throw new IdentityException(IdentityError.REFRESH_TOKEN_INVALID);
        }
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public UUID sessionId() {
        return sessionId;
    }

    public UUID identityId() {
        return identityId;
    }

    public String accessTokenHash() {
        return accessTokenHash;
    }

    public String refreshTokenHash() {
        return refreshTokenHash;
    }

    public IdentityRole role() {
        return role;
    }

    public ScopeType scopeType() {
        return scopeType;
    }

    public String scopeId() {
        return scopeId;
    }

    public ClientApplication clientApplication() {
        return clientApplication;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant accessExpiresAt() {
        return accessExpiresAt;
    }

    public Instant refreshExpiresAt() {
        return refreshExpiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    public Instant lastUsedAt() {
        return lastUsedAt;
    }
}
