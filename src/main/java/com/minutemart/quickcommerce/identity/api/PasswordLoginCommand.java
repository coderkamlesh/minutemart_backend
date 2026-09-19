package com.minutemart.quickcommerce.identity.api;

public record PasswordLoginCommand(
        String phoneNumber,
        String password,
        IdentityRole requestedRole,
        ScopeType requestedScopeType,
        String requestedScopeId,
        ClientApplication clientApplication
) {
}
