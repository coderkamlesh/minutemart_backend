package com.minutemart.quickcommerce.identity.infrastructure;

import com.minutemart.quickcommerce.identity.application.IdentityAuthProperties;
import com.minutemart.quickcommerce.identity.application.IdentityClock;
import com.minutemart.quickcommerce.identity.application.InvitationDeliveryPort;
import com.minutemart.quickcommerce.identity.application.OtpCodeGenerator;
import com.minutemart.quickcommerce.identity.application.OtpDeliveryPort;
import com.minutemart.quickcommerce.identity.application.TokenDigester;
import com.minutemart.quickcommerce.identity.application.TokenGenerator;
import com.minutemart.quickcommerce.identity.domain.Invitation;
import com.minutemart.quickcommerce.identity.domain.PhoneNumber;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(IdentityAuthProperties.class)
public class IdentityConfiguration {

    @Bean
    PasswordEncoder identityPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    IdentityClock identityClock() {
        Clock clock = Clock.systemUTC();
        return clock::instant;
    }

    @Bean
    TokenGenerator identityTokenGenerator() {
        SecureRandom random = new SecureRandom();
        return () -> {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        };
    }

    @Bean
    OtpCodeGenerator identityOtpCodeGenerator() {
        SecureRandom random = new SecureRandom();
        return () -> String.format("%06d", random.nextInt(1_000_000));
    }

    @Bean
    TokenDigester identityTokenDigester() {
        return rawValue -> {
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256")
                        .digest(rawValue.getBytes(StandardCharsets.UTF_8));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException("SHA-256 is not available", exception);
            }
        };
    }

    @Bean
    @Profile("dev")
    OtpDeliveryPort developmentOtpDelivery() {
        return new DevelopmentOtpDelivery();
    }

    @Bean
    @Profile("dev")
    InvitationDeliveryPort developmentInvitationDelivery() {
        return new DevelopmentInvitationDelivery();
    }

    @Bean
    @org.springframework.context.annotation.Profile("!dev")
    OtpDeliveryPort unavailableOtpDelivery(IdentityAuthProperties properties) {
        if (!"development".equalsIgnoreCase(properties.getDeliveryMode())) {
            throw new IllegalStateException("Configure a production OTP delivery adapter before starting identity");
        }
        return new DevelopmentOtpDelivery();
    }

    @Bean
    @org.springframework.context.annotation.Profile("!dev")
    InvitationDeliveryPort unavailableInvitationDelivery(IdentityAuthProperties properties) {
        if (!"development".equalsIgnoreCase(properties.getDeliveryMode())) {
            throw new IllegalStateException("Configure a production invitation delivery adapter before starting identity");
        }
        return new DevelopmentInvitationDelivery();
    }

    private static final class DevelopmentOtpDelivery implements OtpDeliveryPort {

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DevelopmentOtpDelivery.class);

        @Override
        public void send(PhoneNumber phoneNumber, String code, Instant expiresAt) {
            log.info("DEVELOPMENT OTP Delivery -> Phone: {}, Code: {}, ExpiresAt: {}",
                    phoneNumber.masked(), code, expiresAt);
        }
    }

    private static final class DevelopmentInvitationDelivery implements InvitationDeliveryPort {

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DevelopmentInvitationDelivery.class);

        @Override
        public void send(Invitation invitation, String rawToken) {
            log.info("DEVELOPMENT Invitation Delivery -> Phone: {}, Role: {}, Token: {}, ExpiresAt: {}",
                    invitation.phoneNumber().masked(), invitation.role(), rawToken, invitation.expiresAt());
        }
    }
}
