package com.minutemart.quickcommerce.identity.domain;

import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.ScopeType;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class IdentityAccount {

    private final UUID identityId;
    private final PhoneNumber phoneNumber;
    private IdentityStatus status;
    private String passwordHash;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Set<RoleAssignment> roleAssignments;

    private IdentityAccount(
            UUID identityId,
            PhoneNumber phoneNumber,
            IdentityStatus status,
            String passwordHash,
            Instant createdAt,
            Instant updatedAt,
            Set<RoleAssignment> roleAssignments
    ) {
        this.identityId = Objects.requireNonNull(identityId);
        this.phoneNumber = Objects.requireNonNull(phoneNumber);
        this.status = Objects.requireNonNull(status);
        this.passwordHash = passwordHash;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.roleAssignments = new LinkedHashSet<>(roleAssignments);
    }

    public static IdentityAccount register(UUID identityId, PhoneNumber phoneNumber, Instant now) {
        return new IdentityAccount(identityId, phoneNumber, IdentityStatus.ACTIVE,
                null, now, now, Set.of());
    }

    public static IdentityAccount rehydrate(
            UUID identityId,
            PhoneNumber phoneNumber,
            IdentityStatus status,
            String passwordHash,
            Instant createdAt,
            Instant updatedAt,
            Set<RoleAssignment> roleAssignments
    ) {
        return new IdentityAccount(identityId, phoneNumber, status, passwordHash,
                createdAt, updatedAt, roleAssignments);
    }

    public RoleAssignment assignRole(
            UUID assignmentId,
            IdentityRole role,
            ScopeType scopeType,
            String scopeId,
            boolean activateImmediately,
            Instant now
    ) {
        RoleScope.validate(role, scopeType, scopeId);
        if (findAssignment(role, scopeType, scopeId).isPresent()) {
            throw new IdentityException(IdentityError.ROLE_ALREADY_ASSIGNED);
        }

        RoleAssignment assignment = activateImmediately
                ? RoleAssignment.active(assignmentId, role, scopeType, scopeId, now, now)
                : RoleAssignment.pending(assignmentId, role, scopeType, scopeId, now);
        roleAssignments.add(assignment);
        touch(now);
        return assignment;
    }

    public RoleAssignment activateRole(IdentityRole role, ScopeType scopeType, String scopeId, Instant now) {
        RoleAssignment assignment = findAssignment(role, scopeType, scopeId)
                .orElseThrow(() -> new IdentityException(IdentityError.ROLE_NOT_ACTIVE));
        assignment.activate(now);
        touch(now);
        return assignment;
    }

    public RoleAssignment suspendRole(IdentityRole role, ScopeType scopeType, String scopeId, Instant now) {
        RoleAssignment assignment = findAssignment(role, scopeType, scopeId)
                .orElseThrow(() -> new IdentityException(IdentityError.ROLE_NOT_ACTIVE));
        assignment.suspend(now);
        touch(now);
        return assignment;
    }

    public void setPasswordHash(String passwordHash, Instant now) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("password hash must not be blank");
        }
        this.passwordHash = passwordHash;
        touch(now);
    }

    public void block(Instant now) {
        status = IdentityStatus.BLOCKED;
        touch(now);
    }

    public void assertCanAuthenticate() {
        switch (status) {
            case BLOCKED -> throw new IdentityException(IdentityError.IDENTITY_BLOCKED);
            case DELETED -> throw new IdentityException(IdentityError.IDENTITY_DELETED);
            case ACTIVE -> {
            }
        }
    }

    public boolean hasActiveRole(IdentityRole role) {
        return roleAssignments.stream()
                .anyMatch(assignment -> assignment.role() == role && assignment.isActive());
    }

    public Optional<RoleAssignment> activeRole(IdentityRole role, ScopeType scopeType, String scopeId) {
        return roleAssignments.stream()
                .filter(assignment -> assignment.role() == role)
                .filter(assignment -> assignment.scopeType() == scopeType)
                .filter(assignment -> Objects.equals(assignment.scopeId(), scopeId))
                .filter(RoleAssignment::isActive)
                .findFirst();
    }

    public Optional<RoleAssignment> firstActiveRole() {
        return roleAssignments.stream().filter(RoleAssignment::isActive).findFirst();
    }

    public Set<RoleAssignment> activeRoleAssignments() {
        return roleAssignments.stream()
                .filter(RoleAssignment::isActive)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public Optional<RoleAssignment> findAssignment(IdentityRole role, ScopeType scopeType, String scopeId) {
        return roleAssignments.stream()
                .filter(assignment -> assignment.role() == role)
                .filter(assignment -> assignment.scopeType() == scopeType)
                .filter(assignment -> Objects.equals(assignment.scopeId(), scopeId))
                .findFirst();
    }

    private void touch(Instant now) {
        updatedAt = now;
    }

    public UUID identityId() {
        return identityId;
    }

    public PhoneNumber phoneNumber() {
        return phoneNumber;
    }

    public IdentityStatus status() {
        return status;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Set<RoleAssignment> roleAssignments() {
        return Collections.unmodifiableSet(roleAssignments);
    }
}
