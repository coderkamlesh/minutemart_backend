package com.minutemart.quickcommerce.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataIdentityAccountRepository extends JpaRepository<IdentityAccountEntity, UUID> {

    Optional<IdentityAccountEntity> findByPhoneNumber(String phoneNumber);
}
