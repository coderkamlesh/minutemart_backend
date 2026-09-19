package com.minutemart.quickcommerce.identity.application;

import com.minutemart.quickcommerce.identity.domain.OtpChallenge;
import com.minutemart.quickcommerce.identity.domain.PhoneNumber;

import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository {

    Optional<OtpChallenge> findById(UUID challengeId);

    Optional<OtpChallenge> findLatestByPhoneNumber(PhoneNumber phoneNumber);

    OtpChallenge save(OtpChallenge challenge);

    int markVerified(UUID challengeId, java.time.Instant verifiedAt, java.time.Instant now);
}
