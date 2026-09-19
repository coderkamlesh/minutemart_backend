package com.minutemart.quickcommerce.identity.api;

import java.time.Instant;
import java.util.UUID;

public record OtpChallengeIssued(
        UUID challengeId,
        String maskedPhoneNumber,
        Instant expiresAt,
        String developmentCode
) {
}
