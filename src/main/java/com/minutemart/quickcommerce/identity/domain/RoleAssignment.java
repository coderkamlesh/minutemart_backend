package com.minutemart.quickcommerce.identity.domain;

import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.ScopeType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class RoleAssignment {

    private final UUID assignmentId;
    private final IdentityRole role;
    private final ScopeType scopeType;
    private final String scopeId;
    private RoleAssignmentStatus status;
    private final Instant grantedAt;
    private Instant activatedAt;
    private Instant suspendedAt;

    private RoleAssignment(
            UUID assignmentId,
            IdentityRole role,
            ScopeType scopeType,
            String scopeId,
            RoleAssignmentStatus status,
            Instant grantedAt,
            Instant activatedAt,
            Instant suspendedAt
    ) {
        this.assignmentId = Objects.requireNonNull(assignmentId);
        this.role = Objects.requireNonNull(role);
        this.scopeType = Objects.requireNonNull(scopeType);
        this.scopeId = scopeId;
        this.status = Objects.requireNonNull(status);
        this.grantedAt = Objects.requireNonNull(grantedAt);
        this.activatedAt = activatedAt;
        this.suspendedAt = suspendedAt;
    }

    public static RoleAssignment pending(
            UUID assignmentId,
            IdentityRole role,
            ScopeType scopeType,
            String scopeId,
            Instant grantedAt
    ) {
        RoleScope.validate(role, scopeType, scopeId);
        return new RoleAssignment(assignmentId, role, scopeType, scopeId,
                RoleAssignmentStatus.PENDING, grantedAt, null, null);
    }

    public static RoleAssignment active(
            UUID assignmentId,
            IdentityRole role,
            ScopeType scopeType,
            String scopeId,
            Instant grantedAt,
            Instant activatedAt
    ) {
        RoleScope.validate(role, scopeType, scopeId);
        return new RoleAssignment(assignmentId, role, scopeType, scopeId,
                RoleAssignmentStatus.ACTIVE, grantedAt, activatedAt, null);
    }

    public static RoleAssignment rehydrate(
            UUID assignmentId,
            IdentityRole role,
            ScopeType scopeType,
            String scopeId,
            RoleAssignmentStatus status,
            Instant grantedAt,
            Instant activatedAt,
            Instant suspendedAt
    ) {
        RoleScope.validate(role, scopeType, scopeId);
        return new RoleAssignment(assignmentId, role, scopeType, scopeId,
                status, grantedAt, activatedAt, suspendedAt);
    }

    public void activate(Instant now) {
        if (status != RoleAssignmentStatus.PENDING) {
            throw new IdentityException(IdentityError.ROLE_NOT_ACTIVE);
        }
        status = RoleAssignmentStatus.ACTIVE;
        activatedAt = now;
        suspendedAt = null;
    }

    public void suspend(Instant now) {
        if (status != RoleAssignmentStatus.ACTIVE) {
            throw new IdentityException(IdentityError.ROLE_NOT_ACTIVE);
        }
        status = RoleAssignmentStatus.SUSPENDED;
        suspendedAt = now;
    }

    public boolean isActive() {
        return status == RoleAssignmentStatus.ACTIVE;
    }

    public UUID assignmentId() {
        return assignmentId;
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

    public RoleAssignmentStatus status() {
        return status;
    }

    public Instant grantedAt() {
        return grantedAt;
    }

    public Instant activatedAt() {
        return activatedAt;
    }

    public Instant suspendedAt() {
        return suspendedAt;
    }
}
