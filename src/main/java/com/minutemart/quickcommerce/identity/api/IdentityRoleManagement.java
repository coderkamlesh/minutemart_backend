package com.minutemart.quickcommerce.identity.api;

import java.util.UUID;

public interface IdentityRoleManagement {

    void activateRole(UUID identityId, IdentityRole role, ScopeType scopeType, String scopeId, UUID performedBy);

    void suspendRole(UUID identityId, IdentityRole role, ScopeType scopeType, String scopeId, UUID performedBy);
}
