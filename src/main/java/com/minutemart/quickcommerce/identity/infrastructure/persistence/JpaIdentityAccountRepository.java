package com.minutemart.quickcommerce.identity.infrastructure.persistence;

import com.minutemart.quickcommerce.identity.application.IdentityAccountRepository;
import com.minutemart.quickcommerce.identity.domain.IdentityAccount;
import com.minutemart.quickcommerce.identity.domain.PhoneNumber;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaIdentityAccountRepository implements IdentityAccountRepository {

    private final SpringDataIdentityAccountRepository delegate;

    public JpaIdentityAccountRepository(SpringDataIdentityAccountRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<IdentityAccount> findById(UUID identityId) {
        return delegate.findById(identityId).map(IdentityAccountEntity::toDomain);
    }

    @Override
    public Optional<IdentityAccount> findByPhoneNumber(PhoneNumber phoneNumber) {
        return delegate.findByPhoneNumber(phoneNumber.value()).map(IdentityAccountEntity::toDomain);
    }

    @Override
    public IdentityAccount save(IdentityAccount account) {
        IdentityAccountEntity entity = delegate.findById(account.identityId())
                .map(existing -> {
                    existing.apply(account);
                    return existing;
                })
                .orElseGet(() -> IdentityAccountEntity.fromDomain(account));
        return delegate.save(entity).toDomain();
    }
}
