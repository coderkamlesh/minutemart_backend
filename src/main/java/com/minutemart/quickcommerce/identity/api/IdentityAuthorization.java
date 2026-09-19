package com.minutemart.quickcommerce.identity.api;

import java.util.UUID;

public interface IdentityAuthorization {

    boolean hasRole(UUID identityId, IdentityRole role);

    boolean hasScopedRole(UUID identityId, IdentityRole role, ScopeType scopeType, String scopeId);
}
