package com.minutemart.quickcommerce.identity.infrastructure.persistence;

import com.minutemart.quickcommerce.identity.application.OtpChallengeRepository;
import com.minutemart.quickcommerce.identity.domain.OtpChallenge;
import com.minutemart.quickcommerce.identity.domain.PhoneNumber;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaOtpChallengeRepository implements OtpChallengeRepository {

    private final SpringDataOtpChallengeRepository delegate;

    public JpaOtpChallengeRepository(SpringDataOtpChallengeRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<OtpChallenge> findById(UUID challengeId) {
        return delegate.findById(challengeId).map(OtpChallengeEntity::toDomain);
    }

    @Override
    public Optional<OtpChallenge> findLatestByPhoneNumber(PhoneNumber phoneNumber) {
        return delegate.findFirstByPhoneNumberOrderByCreatedAtDesc(phoneNumber.value())
                .map(OtpChallengeEntity::toDomain);
    }

    @Override
    public OtpChallenge save(OtpChallenge challenge) {
        OtpChallengeEntity entity = delegate.findById(challenge.challengeId())
                .map(existing -> {
                    existing.apply(challenge);
                    return existing;
                })
                .orElseGet(() -> OtpChallengeEntity.fromDomain(challenge));
        return delegate.save(entity).toDomain();
    }

    @Override
    public int markVerified(UUID challengeId, java.time.Instant verifiedAt, java.time.Instant now) {
        return delegate.markVerified(challengeId, verifiedAt, now);
    }
}
