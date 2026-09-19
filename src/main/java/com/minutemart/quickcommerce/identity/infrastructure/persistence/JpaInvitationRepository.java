package com.minutemart.quickcommerce.identity.infrastructure.persistence;

import com.minutemart.quickcommerce.identity.application.InvitationRepository;
import com.minutemart.quickcommerce.identity.domain.Invitation;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaInvitationRepository implements InvitationRepository {

    private final SpringDataInvitationRepository delegate;

    public JpaInvitationRepository(SpringDataInvitationRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<Invitation> findByTokenHash(String tokenHash) {
        return delegate.findByTokenHash(tokenHash).map(InvitationEntity::toDomain);
    }

    @Override
    public Invitation save(Invitation invitation) {
        InvitationEntity entity = delegate.findById(invitation.invitationId())
                .map(existing -> {
                    existing.apply(invitation);
                    return existing;
                })
                .orElseGet(() -> InvitationEntity.fromDomain(invitation));
        return delegate.save(entity).toDomain();
    }
}
