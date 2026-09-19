package com.minutemart.quickcommerce.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpringDataOtpChallengeRepository extends JpaRepository<OtpChallengeEntity, UUID> {

    Optional<OtpChallengeEntity> findFirstByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    @Modifying
    @Query("update OtpChallengeEntity challenge set challenge.status = 'VERIFIED', "
            + "challenge.verifiedAt = :verifiedAt where challenge.challengeId = :challengeId "
            + "and challenge.status = 'CREATED' and challenge.expiresAt > :now")
    int markVerified(
            @Param("challengeId") UUID challengeId,
            @Param("verifiedAt") java.time.Instant verifiedAt,
            @Param("now") java.time.Instant now
    );
}
