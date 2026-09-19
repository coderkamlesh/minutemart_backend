package com.minutemart.quickcommerce.identity.application;

import com.minutemart.quickcommerce.identity.api.AcceptInvitationCommand;
import com.minutemart.quickcommerce.identity.api.AssignRoleCommand;
import com.minutemart.quickcommerce.identity.api.AuthenticationResult;
import com.minutemart.quickcommerce.identity.api.ClientApplication;
import com.minutemart.quickcommerce.identity.api.IdentityAdministration;
import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.InvitationIssued;
import com.minutemart.quickcommerce.identity.api.InviteIdentityCommand;
import com.minutemart.quickcommerce.identity.api.RoleAssignmentView;
import com.minutemart.quickcommerce.identity.api.RoleStatus;
import com.minutemart.quickcommerce.identity.api.ScopeType;
import com.minutemart.quickcommerce.identity.domain.IdentityAccount;
import com.minutemart.quickcommerce.identity.domain.IdentityError;
import com.minutemart.quickcommerce.identity.domain.IdentityException;
import com.minutemart.quickcommerce.identity.domain.Invitation;
import com.minutemart.quickcommerce.identity.domain.PhoneNumber;
import com.minutemart.quickcommerce.identity.domain.RoleAssignment;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class IdentityAdministrationApplicationService implements IdentityAdministration {

    private final IdentityAccountRepository identityAccounts;
    private final InvitationRepository invitations;
    private final AuthSessionRepository sessions;
    private final AuthenticationApplicationService authentication;
    private final InvitationDeliveryPort invitationDelivery;
    private final TokenGenerator tokenGenerator;
    private final TokenDigester tokenDigester;
    private final PasswordEncoder passwordEncoder;
    private final IdentityClock clock;
    private final IdentityAuthProperties properties;
    private final ApplicationEventPublisher events;

    public IdentityAdministrationApplicationService(
            IdentityAccountRepository identityAccounts,
            InvitationRepository invitations,
            AuthSessionRepository sessions,
            AuthenticationApplicationService authentication,
            InvitationDeliveryPort invitationDelivery,
            TokenGenerator tokenGenerator,
            TokenDigester tokenDigester,
            PasswordEncoder passwordEncoder,
            IdentityClock clock,
            IdentityAuthProperties properties,
            ApplicationEventPublisher events
    ) {
        this.identityAccounts = identityAccounts;
        this.invitations = invitations;
        this.sessions = sessions;
        this.authentication = authentication;
        this.invitationDelivery = invitationDelivery;
        this.tokenGenerator = tokenGenerator;
        this.tokenDigester = tokenDigester;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.properties = properties;
        this.events = events;
    }

    @Override
    @Transactional
    public InvitationIssued invite(InviteIdentityCommand command, UUID invitedBy) {
        requireInvitableRole(command.role());
        IdentityAccount inviter = identityAccounts.findById(invitedBy)
                .orElseThrow(() -> new IdentityException(IdentityError.IDENTITY_NOT_FOUND));
        if (!inviter.hasActiveRole(IdentityRole.ADMIN)) {
            throw new IdentityException(IdentityError.ROLE_NOT_ACTIVE);
        }
        PhoneNumber phoneNumber = PhoneNumber.of(command.phoneNumber());
        Instant now = clock.now();
        String rawToken = tokenGenerator.generate();
        Instant expiresAt = now.plus(command.validity() == null
                ? properties.getInvitationValidity()
                : command.validity());
        Invitation invitation = Invitation.create(
                UUID.randomUUID(),
                phoneNumber,
                command.role(),
                command.scopeType(),
                command.scopeId(),
                tokenDigester.digest(rawToken),
                invitedBy,
                now,
                expiresAt
        );
        invitations.save(invitation);
        invitationDelivery.send(invitation, rawToken);
        return new InvitationIssued(
                invitation.invitationId(),
                phoneNumber.masked(),
                invitation.role(),
                invitation.scopeType(),
                invitation.scopeId(),
                invitation.expiresAt(),
                properties.isExposeDevelopmentSecrets() ? rawToken : null
        );
    }

    @Override
    @Transactional
    public AuthenticationResult acceptInvitation(AcceptInvitationCommand command) {
        Instant now = clock.now();
        Invitation invitation = invitations.findByTokenHash(tokenDigester.digest(command.invitationToken()))
                .orElseThrow(() -> new IdentityException(IdentityError.INVITATION_NOT_FOUND));
        invitation.ensureAcceptable(now);
        if (command.password() == null || command.password().length() < 12) {
            throw new IdentityException(IdentityError.INVALID_CREDENTIALS);
        }

        IdentityAccount account = identityAccounts.findByPhoneNumber(invitation.phoneNumber())
                .orElseGet(() -> IdentityAccount.register(UUID.randomUUID(), invitation.phoneNumber(), now));
        account.assertCanAuthenticate();
        if (account.hasPassword()) {
            throw new IdentityException(IdentityError.CREDENTIALS_ALREADY_SET);
        }
        account.setPasswordHash(passwordEncoder.encode(command.password()), now);

        boolean newAccount = identityAccounts.findByPhoneNumber(invitation.phoneNumber()).isEmpty();
        account.findAssignment(invitation.role(), invitation.scopeType(), invitation.scopeId())
                .ifPresent(existing -> {
                    throw new IdentityException(existing.isActive()
                            ? IdentityError.ROLE_ALREADY_ASSIGNED
                            : IdentityError.ROLE_NOT_ACTIVE);
                });

        RoleAssignment assignment = account.assignRole(
                UUID.randomUUID(),
                invitation.role(),
                invitation.scopeType(),
                invitation.scopeId(),
                true,
                now
        );

        invitation.accept(now);
        invitations.save(invitation);
        IdentityAccount saved = identityAccounts.save(account);
        if (newAccount) {
            events.publishEvent(new com.minutemart.quickcommerce.identity.events.IdentityCreated(
                    saved.identityId(), now));
        }
        events.publishEvent(new com.minutemart.quickcommerce.identity.events.RoleAssigned(
                assignment.assignmentId(), saved.identityId(), assignment.role().name(), assignment.scopeType().name(),
                assignment.scopeId(), assignment.status().name(), now));
        events.publishEvent(new com.minutemart.quickcommerce.identity.events.RoleActivated(
                assignment.assignmentId(), saved.identityId(), assignment.role().name(), assignment.scopeType().name(),
                assignment.scopeId(), now));

        return authentication.authenticateIdentity(
                saved.identityId(),
                assignment.role(),
                assignment.scopeType(),
                assignment.scopeId(),
                ClientApplication.defaultFor(assignment.role())
        );
    }

    @Override
    @Transactional
    public RoleAssignmentView assignRole(AssignRoleCommand command, UUID performedBy) {
        IdentityAccount performer = identityAccounts.findById(performedBy)
                .orElseThrow(() -> new IdentityException(IdentityError.IDENTITY_NOT_FOUND));
        if (!performer.hasActiveRole(IdentityRole.ADMIN)) {
            throw new IdentityException(IdentityError.ROLE_NOT_ACTIVE);
        }

        IdentityAccount account = identityAccounts.findById(command.identityId())
                .orElseThrow(() -> new IdentityException(IdentityError.IDENTITY_NOT_FOUND));
        Instant now = clock.now();
        RoleAssignment assignment = account.assignRole(
                UUID.randomUUID(),
                command.role(),
                command.scopeType(),
                command.scopeId(),
                command.activateImmediately(),
                now
        );
        identityAccounts.save(account);
        events.publishEvent(new com.minutemart.quickcommerce.identity.events.RoleAssigned(
                assignment.assignmentId(), account.identityId(), assignment.role().name(), assignment.scopeType().name(),
                assignment.scopeId(), assignment.status().name(), now));
        if (assignment.isActive()) {
            events.publishEvent(new com.minutemart.quickcommerce.identity.events.RoleActivated(
                    assignment.assignmentId(), account.identityId(), assignment.role().name(), assignment.scopeType().name(),
                    assignment.scopeId(), now));
        }
        return toView(account.identityId(), assignment);
    }

    @Override
    @Transactional
    public void blockIdentity(UUID identityId, UUID performedBy) {
        IdentityAccount performer = identityAccounts.findById(performedBy)
                .orElseThrow(() -> new IdentityException(IdentityError.IDENTITY_NOT_FOUND));
        if (!performer.hasActiveRole(IdentityRole.ADMIN)) {
            throw new IdentityException(IdentityError.ROLE_NOT_ACTIVE);
        }
        IdentityAccount account = identityAccounts.findById(identityId)
                .orElseThrow(() -> new IdentityException(IdentityError.IDENTITY_NOT_FOUND));
        Instant now = clock.now();
        account.block(now);
        identityAccounts.save(account);
        sessions.revokeAllForIdentity(identityId, now);
        events.publishEvent(new com.minutemart.quickcommerce.identity.events.IdentityBlocked(identityId, now));
    }

    private void requireInvitableRole(IdentityRole role) {
        if (role == null || role == IdentityRole.CONSUMER || role == IdentityRole.RIDER) {
            throw new IdentityException(IdentityError.INVALID_INVITATION_ROLE);
        }
    }

    private RoleAssignmentView toView(UUID identityId, RoleAssignment assignment) {
        return new RoleAssignmentView(
                assignment.assignmentId(),
                identityId,
                assignment.role(),
                assignment.scopeType(),
                assignment.scopeId(),
                RoleStatus.valueOf(assignment.status().name()),
                assignment.grantedAt(),
                assignment.activatedAt(),
                assignment.suspendedAt()
        );
    }
}
