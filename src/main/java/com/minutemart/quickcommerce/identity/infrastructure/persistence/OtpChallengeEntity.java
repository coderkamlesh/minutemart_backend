package com.minutemart.quickcommerce.identity.infrastructure.persistence;

import com.minutemart.quickcommerce.identity.domain.ChallengePurpose;
import com.minutemart.quickcommerce.identity.domain.ChallengeStatus;
import com.minutemart.quickcommerce.identity.domain.OtpChallenge;
import com.minutemart.quickcommerce.identity.domain.PhoneNumber;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identity_otp_challenge")
class OtpChallengeEntity {

    @Id
    @Column(name = "challenge_id", nullable = false)
    private UUID challengeId;

    @Column(name = "phone_number", nullable = false, length = 16)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 32)
    private ChallengePurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 128)
    private String codeHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ChallengeStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    protected OtpChallengeEntity() {
    }

    static OtpChallengeEntity fromDomain(OtpChallenge challenge) {
        OtpChallengeEntity entity = new OtpChallengeEntity();
        entity.apply(challenge);
        return entity;
    }

    void apply(OtpChallenge challenge) {
        challengeId = challenge.challengeId();
        phoneNumber = challenge.phoneNumber().value();
        purpose = challenge.purpose();
        codeHash = challenge.codeHash();
        createdAt = challenge.createdAt();
        expiresAt = challenge.expiresAt();
        maxAttempts = challenge.maxAttempts();
        status = challenge.status();
        attempts = challenge.attempts();
        verifiedAt = challenge.verifiedAt();
    }

    OtpChallenge toDomain() {
        return OtpChallenge.rehydrate(
                challengeId,
                PhoneNumber.of(phoneNumber),
                purpose,
                codeHash,
                createdAt,
                expiresAt,
                maxAttempts,
                status,
                attempts,
                verifiedAt
        );
    }
}
