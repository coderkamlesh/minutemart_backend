package com.minutemart.quickcommerce.identity.application;

import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.IdentityRoleManagement;
import com.minutemart.quickcommerce.identity.api.ScopeType;
import com.minutemart.quickcommerce.identity.domain.IdentityAccount;
import com.minutemart.quickcommerce.identity.domain.RoleAssignment;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class IdentityRoleManagementApplicationService implements IdentityRoleManagement {

    private final IdentityAccountRepository identityAccounts;
    private final AuthSessionRepository sessions;
    private final IdentityClock clock;
    private final ApplicationEventPublisher events;

    public IdentityRoleManagementApplicationService(
            IdentityAccountRepository identityAccounts,
            AuthSessionRepository sessions,
            IdentityClock clock,
            ApplicationEventPublisher events
    ) {
        this.identityAccounts = identityAccounts;
        this.sessions = sessions;
        this.clock = clock;
        this.events = events;
    }

    @Override
    @Transactional
    public void activateRole(UUID identityId, IdentityRole role, ScopeType scopeType, String scopeId, UUID performedBy) {
        assertAdmin(performedBy);
        IdentityAccount account = load(identityId);
        Instant now = clock.now();
        RoleAssignment assignment = account.activateRole(role, scopeType, scopeId, now);
        identityAccounts.save(account);
        events.publishEvent(new com.minutemart.quickcommerce.identity.events.RoleActivated(
                assignment.assignmentId(), identityId, role.name(), scopeType.name(), scopeId, now));
    }

    @Override
    @Transactional
    public void suspendRole(UUID identityId, IdentityRole role, ScopeType scopeType, String scopeId, UUID performedBy) {
        assertAdmin(performedBy);
        IdentityAccount account = load(identityId);
        Instant now = clock.now();
        account.suspendRole(role, scopeType, scopeId, now);
        identityAccounts.save(account);
        sessions.revokeForIdentityAndRole(identityId, role, scopeType, scopeId, now);
        events.publishEvent(new com.minutemart.quickcommerce.identity.events.RoleSuspended(
                account.findAssignment(role, scopeType, scopeId).orElseThrow().assignmentId(),
                identityId, role.name(), scopeType.name(), scopeId, now));
    }

    private void assertAdmin(UUID identityId) {
        if (!load(identityId).hasActiveRole(IdentityRole.ADMIN)) {
            throw new com.minutemart.quickcommerce.identity.domain.IdentityException(
                    com.minutemart.quickcommerce.identity.domain.IdentityError.ROLE_NOT_ACTIVE);
        }
    }

    private IdentityAccount load(UUID identityId) {
        return identityAccounts.findById(identityId)
                .orElseThrow(() -> new com.minutemart.quickcommerce.identity.domain.IdentityException(
                        com.minutemart.quickcommerce.identity.domain.IdentityError.IDENTITY_NOT_FOUND));
    }
}
