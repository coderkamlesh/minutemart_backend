package com.minutemart.quickcommerce.identity.events;

import java.time.Instant;
import java.util.UUID;

public record RoleSuspended(
        UUID assignmentId,
        UUID identityId,
        String role,
        String scopeType,
        String scopeId,
        Instant occurredAt
) {
}
