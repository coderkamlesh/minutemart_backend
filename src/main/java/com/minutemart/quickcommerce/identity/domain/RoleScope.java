package com.minutemart.quickcommerce.identity.domain;

import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.ScopeType;

public final class RoleScope {

    private RoleScope() {
    }

    public static void validate(IdentityRole role, ScopeType scopeType, String scopeId) {
        if (role == null || scopeType == null) {
            throw new IdentityException(IdentityError.INVALID_ROLE_SCOPE);
        }

        ScopeType expected = switch (role) {
            case ADMIN, CATALOG_ADMIN, CONSUMER -> ScopeType.GLOBAL;
            case RIDER -> ScopeType.RIDER;
            case SELLER -> ScopeType.SELLER;
            case STORE_MANAGER -> ScopeType.STORE;
            case WAREHOUSE_MANAGER -> ScopeType.WAREHOUSE;
        };

        boolean validScopeId = expected == ScopeType.GLOBAL
                ? scopeId == null || scopeId.isBlank()
                : scopeId != null && !scopeId.isBlank();

        if (expected != scopeType || !validScopeId) {
            throw new IdentityException(IdentityError.INVALID_ROLE_SCOPE);
        }
    }
}
