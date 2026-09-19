package com.minutemart.quickcommerce.identity.api;

public record RequestOtpCommand(
        String phoneNumber,
        IdentityRole requestedRole,
        ScopeType requestedScopeType,
        String requestedScopeId,
        ClientApplication clientApplication
) {
}
