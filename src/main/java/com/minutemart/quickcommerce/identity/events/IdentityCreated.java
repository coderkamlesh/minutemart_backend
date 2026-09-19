package com.minutemart.quickcommerce.identity.events;

import java.time.Instant;
import java.util.UUID;

public record IdentityCreated(UUID identityId, Instant occurredAt) {
}
