package com.minutemart.quickcommerce.identity.domain;

public final class IdentityException extends RuntimeException {

    private final IdentityError error;

    public IdentityException(IdentityError error) {
        super(error.name());
        this.error = error;
    }

    public IdentityError error() {
        return error;
    }
}
