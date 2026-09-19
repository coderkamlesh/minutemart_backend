package com.minutemart.quickcommerce.identity.events;

import java.time.Instant;
import java.util.UUID;

public record RoleAssigned(
        UUID assignmentId,
        UUID identityId,
        String role,
        String scopeType,
        String scopeId,
        String status,
        Instant occurredAt
) {
}
