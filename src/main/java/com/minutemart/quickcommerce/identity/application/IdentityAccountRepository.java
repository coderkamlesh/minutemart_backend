package com.minutemart.quickcommerce.identity.application;

import com.minutemart.quickcommerce.identity.domain.IdentityAccount;
import com.minutemart.quickcommerce.identity.domain.PhoneNumber;

import java.util.Optional;
import java.util.UUID;

public interface IdentityAccountRepository {

    Optional<IdentityAccount> findById(UUID identityId);

    Optional<IdentityAccount> findByPhoneNumber(PhoneNumber phoneNumber);

    IdentityAccount save(IdentityAccount account);
}
