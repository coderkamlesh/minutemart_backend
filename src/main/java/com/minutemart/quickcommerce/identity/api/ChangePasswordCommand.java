package com.minutemart.quickcommerce.identity.api;

import java.util.Objects;
import java.util.UUID;

public record ChangePasswordCommand(
        UUID identityId,
        String currentPassword,
        String newPassword
) {

    public ChangePasswordCommand {
        Objects.requireNonNull(identityId, "identityId must not be null");
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("currentPassword must not be blank");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("newPassword must not be blank");
        }
    }
}
