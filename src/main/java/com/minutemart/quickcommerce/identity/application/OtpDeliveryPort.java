package com.minutemart.quickcommerce.identity.application;

import com.minutemart.quickcommerce.identity.domain.PhoneNumber;

import java.time.Instant;

public interface OtpDeliveryPort {

    void send(PhoneNumber phoneNumber, String code, Instant expiresAt);
}
