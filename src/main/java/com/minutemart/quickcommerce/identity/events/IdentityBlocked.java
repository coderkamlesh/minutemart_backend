package com.minutemart.quickcommerce.identity.events;

import java.time.Instant;
import java.util.UUID;

public record IdentityBlocked(UUID identityId, Instant occurredAt) {
}
