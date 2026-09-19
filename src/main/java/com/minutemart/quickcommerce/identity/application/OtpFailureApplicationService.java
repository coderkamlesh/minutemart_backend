package com.minutemart.quickcommerce.identity.application;

import com.minutemart.quickcommerce.identity.domain.IdentityError;
import com.minutemart.quickcommerce.identity.domain.OtpChallenge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OtpFailureApplicationService {

    private final OtpChallengeRepository challenges;
    private final IdentityClock clock;

    public OtpFailureApplicationService(OtpChallengeRepository challenges, IdentityClock clock) {
        this.challenges = challenges;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdentityError recordInvalidAttempt(UUID challengeId) {
        OtpChallenge challenge = challenges.findById(challengeId)
                .orElse(null);
        if (challenge == null) {
            return IdentityError.OTP_CHALLENGE_NOT_FOUND;
        }
        Instant now = clock.now();
        if (!challenge.expiresAt().isAfter(now)) {
            return IdentityError.OTP_EXPIRED;
        }
        IdentityError error = challenge.recordInvalidAttempt();
        challenges.save(challenge);
        return error;
    }
}
