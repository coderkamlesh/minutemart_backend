package com.minutemart.quickcommerce.identity.application;

import java.time.Instant;

public interface IdentityClock {

    Instant now();
}
