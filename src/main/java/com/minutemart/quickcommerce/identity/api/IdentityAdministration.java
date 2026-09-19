package com.minutemart.quickcommerce.identity.api;

import java.util.UUID;

public interface IdentityAdministration {

    InvitationIssued invite(InviteIdentityCommand command, UUID invitedBy);

    AuthenticationResult acceptInvitation(AcceptInvitationCommand command);

    RoleAssignmentView assignRole(AssignRoleCommand command, UUID performedBy);

    void blockIdentity(UUID identityId, UUID performedBy);
}
