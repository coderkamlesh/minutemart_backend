package com.minutemart.quickcommerce.identity.api;

import java.util.UUID;

public record AssignRoleCommand(
        UUID identityId,
        IdentityRole role,
        ScopeType scopeType,
        String scopeId,
        boolean activateImmediately
) {
}
