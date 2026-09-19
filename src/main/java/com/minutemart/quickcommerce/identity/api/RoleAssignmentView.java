package com.minutemart.quickcommerce.identity.api;

import java.time.Instant;
import java.util.UUID;

public record RoleAssignmentView(
        UUID assignmentId,
        UUID identityId,
        IdentityRole role,
        ScopeType scopeType,
        String scopeId,
        RoleStatus status,
        Instant grantedAt,
        Instant activatedAt,
        Instant suspendedAt
) {
}
