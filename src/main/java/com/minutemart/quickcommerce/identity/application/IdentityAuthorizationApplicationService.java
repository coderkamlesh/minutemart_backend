package com.minutemart.quickcommerce.identity.application;

import com.minutemart.quickcommerce.identity.api.IdentityAuthorization;
import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.ScopeType;
import com.minutemart.quickcommerce.identity.domain.IdentityAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IdentityAuthorizationApplicationService implements IdentityAuthorization {

    private final IdentityAccountRepository identityAccounts;

    public IdentityAuthorizationApplicationService(IdentityAccountRepository identityAccounts) {
        this.identityAccounts = identityAccounts;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(UUID identityId, IdentityRole role) {
        return identityAccounts.findById(identityId)
                .map(account -> account.hasActiveRole(role))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasScopedRole(UUID identityId, IdentityRole role, ScopeType scopeType, String scopeId) {
        return identityAccounts.findById(identityId)
                .flatMap(account -> account.activeRole(role, scopeType, scopeId))
                .isPresent();
    }
}
