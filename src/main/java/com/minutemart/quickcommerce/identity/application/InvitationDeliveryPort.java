package com.minutemart.quickcommerce.identity.application;

import com.minutemart.quickcommerce.identity.domain.Invitation;

public interface InvitationDeliveryPort {

    void send(Invitation invitation, String rawToken);
}
