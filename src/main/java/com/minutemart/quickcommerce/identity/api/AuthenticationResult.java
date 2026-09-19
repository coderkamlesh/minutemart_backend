package com.minutemart.quickcommerce.identity.api;

import java.time.Instant;
import java.util.UUID;

public record AuthenticationResult(
        UUID identityId,
        UUID sessionId,
        IdentityRole role,
        ScopeType scopeType,
        String scopeId,
        ClientApplication clientApplication,
        String tokenType,
        String accessToken,
        Instant accessExpiresAt,
        String refreshToken,
        Instant refreshExpiresAt
) {
}
