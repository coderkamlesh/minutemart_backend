package com.minutemart.quickcommerce.identity.events;

import java.time.Instant;
import java.util.UUID;

public record ContactVerified(UUID identityId, String contactType, Instant occurredAt) {
}
