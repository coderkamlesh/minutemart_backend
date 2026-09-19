package com.minutemart.quickcommerce.identity.api;

import java.time.Instant;
import java.util.UUID;

public record InvitationIssued(
        UUID invitationId,
        String maskedPhoneNumber,
        IdentityRole role,
        ScopeType scopeType,
        String scopeId,
        Instant expiresAt,
        String developmentToken
) {
}
