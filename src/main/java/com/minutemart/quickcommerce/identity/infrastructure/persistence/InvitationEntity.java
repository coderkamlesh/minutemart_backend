package com.minutemart.quickcommerce.identity.infrastructure.persistence;

import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.ScopeType;
import com.minutemart.quickcommerce.identity.domain.Invitation;
import com.minutemart.quickcommerce.identity.domain.InvitationStatus;
import com.minutemart.quickcommerce.identity.domain.PhoneNumber;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identity_invitation")
class InvitationEntity {

    @Id
    @Column(name = "invitation_id", nullable = false)
    private UUID invitationId;

    @Column(name = "phone_number", nullable = false, length = 16)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private IdentityRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private ScopeType scopeType;

    @Column(name = "scope_id", nullable = false, length = 255)
    private String scopeId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InvitationStatus status;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    protected InvitationEntity() {
    }

    static InvitationEntity fromDomain(Invitation invitation) {
        InvitationEntity entity = new InvitationEntity();
        entity.apply(invitation);
        return entity;
    }

    void apply(Invitation invitation) {
        invitationId = invitation.invitationId();
        phoneNumber = invitation.phoneNumber().value();
        role = invitation.role();
        scopeType = invitation.scopeType();
        scopeId = invitation.scopeId() == null ? "" : invitation.scopeId();
        tokenHash = invitation.tokenHash();
        invitedBy = invitation.invitedBy();
        createdAt = invitation.createdAt();
        expiresAt = invitation.expiresAt();
        status = invitation.status();
        acceptedAt = invitation.acceptedAt();
    }

    Invitation toDomain() {
        return Invitation.rehydrate(
                invitationId,
                PhoneNumber.of(phoneNumber),
                role,
                scopeType,
                scopeId.isBlank() ? null : scopeId,
                tokenHash,
                invitedBy,
                createdAt,
                expiresAt,
                status,
                acceptedAt
        );
    }
}
