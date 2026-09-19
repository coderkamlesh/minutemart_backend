package com.minutemart.quickcommerce.identity.api;

import java.util.UUID;

public interface IdentityAuthentication {

    AuthenticatedIdentity authenticateAccessToken(String accessToken);

    void changePassword(ChangePasswordCommand command);
}
