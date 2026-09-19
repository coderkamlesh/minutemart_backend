package com.minutemart.quickcommerce.identity.infrastructure.bootstrap;

import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.ScopeType;
import com.minutemart.quickcommerce.identity.application.IdentityAccountRepository;
import com.minutemart.quickcommerce.identity.application.IdentityAuthProperties;
import com.minutemart.quickcommerce.identity.application.IdentityClock;
import com.minutemart.quickcommerce.identity.domain.IdentityAccount;
import com.minutemart.quickcommerce.identity.domain.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class IdentityBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IdentityBootstrapRunner.class);

    private final IdentityAccountRepository identityAccounts;
    private final IdentityAuthProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final IdentityClock clock;

    public IdentityBootstrapRunner(
            IdentityAccountRepository identityAccounts,
            IdentityAuthProperties properties,
            PasswordEncoder passwordEncoder,
            IdentityClock clock
    ) {
        this.identityAccounts = identityAccounts;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String phone = properties.getBootstrapAdminPhone();
        String password = properties.getBootstrapAdminPassword();

        if (phone == null || phone.isBlank() || password == null || password.isBlank()) {
            return;
        }

        PhoneNumber phoneNumber = PhoneNumber.of(phone);
        if (identityAccounts.findByPhoneNumber(phoneNumber).isPresent()) {
            return;
        }

        Instant now = clock.now();
        UUID adminId = UUID.randomUUID();
        IdentityAccount admin = IdentityAccount.register(adminId, phoneNumber, now);
        admin.setPasswordHash(passwordEncoder.encode(password), now);
        admin.assignRole(
                UUID.randomUUID(),
                IdentityRole.ADMIN,
                ScopeType.GLOBAL,
                null,
                true,
                now
        );

        identityAccounts.save(admin);
        log.info("Bootstrapped initial system ADMIN account for phone: {}", phoneNumber.masked());
    }
}
