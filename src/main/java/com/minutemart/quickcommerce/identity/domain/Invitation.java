package com.minutemart.quickcommerce.identity.domain;

import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.ScopeType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Invitation {

    private final UUID invitationId;
    private final PhoneNumber phoneNumber;
    private final IdentityRole role;
    private final ScopeType scopeType;
    private final String scopeId;
    private final String tokenHash;
    private final UUID invitedBy;
    private final Instant createdAt;
    private final Instant expiresAt;
    private InvitationStatus status;
    private Instant acceptedAt;

    private Invitation(
            UUID invitationId,
            PhoneNumber phoneNumber,
            IdentityRole role,
            ScopeType scopeType,
            String scopeId,
            String tokenHash,
            UUID invitedBy,
            Instant createdAt,
            Instant expiresAt,
            InvitationStatus status,
            Instant acceptedAt
    ) {
        this.invitationId = Objects.requireNonNull(invitationId);
        this.phoneNumber = Objects.requireNonNull(phoneNumber);
        this.role = Objects.requireNonNull(role);
        this.scopeType = Objects.requireNonNull(scopeType);
        this.scopeId = scopeId;
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.invitedBy = Objects.requireNonNull(invitedBy);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.status = Objects.requireNonNull(status);
        this.acceptedAt = acceptedAt;
        RoleScope.validate(role, scopeType, scopeId);
    }

    public static Invitation create(
            UUID invitationId,
            PhoneNumber phoneNumber,
            IdentityRole role,
            ScopeType scopeType,
            String scopeId,
            String tokenHash,
            UUID invitedBy,
            Instant createdAt,
            Instant expiresAt
    ) {
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("invitation must expire in the future");
        }
        return new Invitation(invitationId, phoneNumber, role, scopeType, scopeId,
                tokenHash, invitedBy, createdAt, expiresAt, InvitationStatus.CREATED, null);
    }

    public static Invitation rehydrate(
            UUID invitationId,
            PhoneNumber phoneNumber,
            IdentityRole role,
            ScopeType scopeType,
            String scopeId,
            String tokenHash,
            UUID invitedBy,
            Instant createdAt,
            Instant expiresAt,
            InvitationStatus status,
            Instant acceptedAt
    ) {
        return new Invitation(invitationId, phoneNumber, role, scopeType, scopeId,
                tokenHash, invitedBy, createdAt, expiresAt, status, acceptedAt);
    }

    public void ensureAcceptable(Instant now) {
        if (status == InvitationStatus.ACCEPTED) {
            throw new IdentityException(IdentityError.INVITATION_ALREADY_ACCEPTED);
        }
        if (status == InvitationStatus.REVOKED) {
            throw new IdentityException(IdentityError.INVITATION_REVOKED);
        }
        if (!expiresAt.isAfter(now)) {
            status = InvitationStatus.EXPIRED;
            throw new IdentityException(IdentityError.INVITATION_EXPIRED);
        }
        if (status != InvitationStatus.CREATED) {
            throw new IdentityException(IdentityError.INVITATION_NOT_FOUND);
        }
    }

    public void accept(Instant now) {
        status = InvitationStatus.ACCEPTED;
        acceptedAt = now;
    }

    public UUID invitationId() {
        return invitationId;
    }

    public PhoneNumber phoneNumber() {
        return phoneNumber;
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

    public String tokenHash() {
        return tokenHash;
    }

    public UUID invitedBy() {
        return invitedBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public InvitationStatus status() {
        return status;
    }

    public Instant acceptedAt() {
        return acceptedAt;
    }
}
