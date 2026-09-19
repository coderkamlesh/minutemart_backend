package com.minutemart.quickcommerce.identity.events;

import java.time.Instant;
import java.util.UUID;

public record RoleActivated(
        UUID assignmentId,
        UUID identityId,
        String role,
        String scopeType,
        String scopeId,
        Instant occurredAt
) {
}
