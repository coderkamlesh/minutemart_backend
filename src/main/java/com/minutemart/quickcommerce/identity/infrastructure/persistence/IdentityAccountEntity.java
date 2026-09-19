package com.minutemart.quickcommerce.identity.infrastructure.persistence;

import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.ScopeType;
import com.minutemart.quickcommerce.identity.domain.IdentityAccount;
import com.minutemart.quickcommerce.identity.domain.IdentityStatus;
import com.minutemart.quickcommerce.identity.domain.PhoneNumber;
import com.minutemart.quickcommerce.identity.domain.RoleAssignment;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "identity_account")
class IdentityAccountEntity {

    @Id
    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    @Column(name = "phone_number", nullable = false, unique = true, length = 16)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private IdentityStatus status;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "identity",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<RoleAssignmentEntity> roleAssignments = new LinkedHashSet<>();

    protected IdentityAccountEntity() {
    }

    static IdentityAccountEntity fromDomain(IdentityAccount account) {
        IdentityAccountEntity entity = new IdentityAccountEntity();
        entity.apply(account);
        return entity;
    }

    void apply(IdentityAccount account) {
        identityId = account.identityId();
        phoneNumber = account.phoneNumber().value();
        status = account.status();
        passwordHash = account.passwordHash();
        createdAt = account.createdAt();
        updatedAt = account.updatedAt();
        Map<UUID, RoleAssignmentEntity> existing = roleAssignments.stream()
                .collect(Collectors.toMap(RoleAssignmentEntity::assignmentId, role -> role));
        roleAssignments.removeIf(role -> account.roleAssignments().stream()
                .noneMatch(assignment -> assignment.assignmentId().equals(role.assignmentId())));
        account.roleAssignments().forEach(assignment -> {
            RoleAssignmentEntity entity = existing.get(assignment.assignmentId());
            if (entity == null) {
                entity = RoleAssignmentEntity.fromDomain(this, assignment);
                roleAssignments.add(entity);
            } else {
                entity.apply(this, assignment);
            }
        });
    }

    IdentityAccount toDomain() {
        Set<RoleAssignment> roles = roleAssignments.stream()
                .map(RoleAssignmentEntity::toDomain)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return IdentityAccount.rehydrate(
                identityId,
                PhoneNumber.of(phoneNumber),
                status,
                passwordHash,
                createdAt,
                updatedAt,
                roles
        );
    }
}
