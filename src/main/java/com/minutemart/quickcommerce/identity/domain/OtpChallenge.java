package com.minutemart.quickcommerce.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class OtpChallenge {

    private final UUID challengeId;
    private final PhoneNumber phoneNumber;
    private final ChallengePurpose purpose;
    private final String codeHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final int maxAttempts;
    private ChallengeStatus status;
    private int attempts;
    private Instant verifiedAt;

    private OtpChallenge(
            UUID challengeId,
            PhoneNumber phoneNumber,
            ChallengePurpose purpose,
            String codeHash,
            Instant createdAt,
            Instant expiresAt,
            int maxAttempts,
            ChallengeStatus status,
            int attempts,
            Instant verifiedAt
    ) {
        this.challengeId = Objects.requireNonNull(challengeId);
        this.phoneNumber = Objects.requireNonNull(phoneNumber);
        this.purpose = Objects.requireNonNull(purpose);
        this.codeHash = Objects.requireNonNull(codeHash);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.maxAttempts = maxAttempts;
        this.status = Objects.requireNonNull(status);
        this.attempts = attempts;
        this.verifiedAt = verifiedAt;
    }

    public static OtpChallenge issue(
            UUID challengeId,
            PhoneNumber phoneNumber,
            ChallengePurpose purpose,
            String codeHash,
            Instant createdAt,
            Instant expiresAt,
            int maxAttempts
    ) {
        if (maxAttempts < 1 || !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("invalid OTP challenge policy");
        }
        return new OtpChallenge(challengeId, phoneNumber, purpose, codeHash,
                createdAt, expiresAt, maxAttempts, ChallengeStatus.CREATED, 0, null);
    }

    public static OtpChallenge rehydrate(
            UUID challengeId,
            PhoneNumber phoneNumber,
            ChallengePurpose purpose,
            String codeHash,
            Instant createdAt,
            Instant expiresAt,
            int maxAttempts,
            ChallengeStatus status,
            int attempts,
            Instant verifiedAt
    ) {
        return new OtpChallenge(challengeId, phoneNumber, purpose, codeHash,
                createdAt, expiresAt, maxAttempts, status, attempts, verifiedAt);
    }

    public void ensureVerifiable(Instant now) {
        if (status == ChallengeStatus.VERIFIED) {
            throw new IdentityException(IdentityError.OTP_INVALID);
        }
        if (status == ChallengeStatus.LOCKED || attempts >= maxAttempts) {
            status = ChallengeStatus.LOCKED;
            throw new IdentityException(IdentityError.OTP_ATTEMPTS_EXCEEDED);
        }
        if (!expiresAt.isAfter(now)) {
            status = ChallengeStatus.EXPIRED;
            throw new IdentityException(IdentityError.OTP_EXPIRED);
        }
        if (status != ChallengeStatus.CREATED) {
            throw new IdentityException(IdentityError.OTP_INVALID);
        }
    }

    public IdentityError recordInvalidAttempt() {
        attempts++;
        if (attempts >= maxAttempts) {
            status = ChallengeStatus.LOCKED;
        }
        return attempts >= maxAttempts
                ? IdentityError.OTP_ATTEMPTS_EXCEEDED
                : IdentityError.OTP_INVALID;
    }

    public void markVerified(Instant now) {
        status = ChallengeStatus.VERIFIED;
        verifiedAt = now;
    }

    public UUID challengeId() {
        return challengeId;
    }

    public PhoneNumber phoneNumber() {
        return phoneNumber;
    }

    public ChallengePurpose purpose() {
        return purpose;
    }

    public String codeHash() {
        return codeHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public ChallengeStatus status() {
        return status;
    }

    public int attempts() {
        return attempts;
    }

    public Instant verifiedAt() {
        return verifiedAt;
    }
}
