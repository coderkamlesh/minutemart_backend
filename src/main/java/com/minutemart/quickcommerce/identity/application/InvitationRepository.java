package com.minutemart.quickcommerce.identity.application;

import com.minutemart.quickcommerce.identity.domain.Invitation;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository {

    Optional<Invitation> findByTokenHash(String tokenHash);

    Invitation save(Invitation invitation);
}
