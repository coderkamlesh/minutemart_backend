package com.minutemart.quickcommerce.identity.api;

import java.time.Duration;
import java.util.UUID;

public record InviteIdentityCommand(
        String phoneNumber,
        IdentityRole role,
        ScopeType scopeType,
        String scopeId,
        Duration validity
) {
}
