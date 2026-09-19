package com.minutemart.quickcommerce.identity.infrastructure.persistence;

import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.ScopeType;
import com.minutemart.quickcommerce.identity.domain.RoleAssignment;
import com.minutemart.quickcommerce.identity.domain.RoleAssignmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identity_role_assignment")
class RoleAssignmentEntity {

    @Id
    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identity_id", nullable = false)
    private IdentityAccountEntity identity;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private IdentityRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private ScopeType scopeType;

    @Column(name = "scope_id", nullable = false, length = 255)
    private String scopeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RoleAssignmentStatus status;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    protected RoleAssignmentEntity() {
    }

    static RoleAssignmentEntity fromDomain(IdentityAccountEntity identity, RoleAssignment assignment) {
        RoleAssignmentEntity entity = new RoleAssignmentEntity();
        entity.apply(identity, assignment);
        return entity;
    }

    void apply(IdentityAccountEntity identity, RoleAssignment assignment) {
        assignmentId = assignment.assignmentId();
        this.identity = identity;
        role = assignment.role();
        scopeType = assignment.scopeType();
        scopeId = normalizeScopeId(assignment.scopeId());
        status = assignment.status();
        grantedAt = assignment.grantedAt();
        activatedAt = assignment.activatedAt();
        suspendedAt = assignment.suspendedAt();
    }

    UUID assignmentId() {
        return assignmentId;
    }

    RoleAssignment toDomain() {
        return RoleAssignment.rehydrate(
                assignmentId,
                role,
                scopeType,
                denormalizeScopeId(scopeId),
                status,
                grantedAt,
                activatedAt,
                suspendedAt
        );
    }

    private static String normalizeScopeId(String scopeId) {
        return scopeId == null ? "" : scopeId;
    }

    private static String denormalizeScopeId(String scopeId) {
        return scopeId == null || scopeId.isBlank() ? null : scopeId;
    }
}
