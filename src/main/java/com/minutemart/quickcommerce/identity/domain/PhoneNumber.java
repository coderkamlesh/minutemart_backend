package com.minutemart.quickcommerce.identity.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record PhoneNumber(String value) {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    public PhoneNumber {
        Objects.requireNonNull(value, "phone number must not be null");
        if (!E164.matcher(value).matches()) {
            throw new IdentityException(IdentityError.INVALID_PHONE_NUMBER);
        }
    }

    public static PhoneNumber of(String value) {
        if (value == null) {
            throw new IdentityException(IdentityError.INVALID_PHONE_NUMBER);
        }
        return new PhoneNumber(value.trim());
    }

    public String masked() {
        if (value.length() <= 4) {
            return "****";
        }
        return "*".repeat(Math.max(0, value.length() - 4)) + value.substring(value.length() - 4);
    }
}
