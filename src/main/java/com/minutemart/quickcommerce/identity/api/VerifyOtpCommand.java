package com.minutemart.quickcommerce.identity.api;

import java.util.UUID;

public record VerifyOtpCommand(
        UUID challengeId,
        String code,
        IdentityRole requestedRole,
        ScopeType requestedScopeType,
        String requestedScopeId,
        ClientApplication clientApplication
) {
}
