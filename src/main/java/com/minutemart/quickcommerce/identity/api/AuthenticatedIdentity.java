package com.minutemart.quickcommerce.identity.api;

import java.util.UUID;

public record AuthenticatedIdentity(
        UUID identityId,
        UUID sessionId,
        IdentityRole role,
        ScopeType scopeType,
        String scopeId,
        ClientApplication clientApplication
) {
}
