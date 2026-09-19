package com.minutemart.quickcommerce.identity.api;

public record AcceptInvitationCommand(String invitationToken, String password) {
}
