package com.minutemart.quickcommerce.identity.web;

import com.minutemart.quickcommerce.identity.api.AcceptInvitationCommand;
import com.minutemart.quickcommerce.identity.api.AssignRoleCommand;
import com.minutemart.quickcommerce.identity.api.AuthenticatedIdentity;
import com.minutemart.quickcommerce.identity.api.AuthenticationResult;
import com.minutemart.quickcommerce.identity.api.ChangePasswordCommand;
import com.minutemart.quickcommerce.identity.api.ClientApplication;
import com.minutemart.quickcommerce.identity.api.IdentityAdministration;
import com.minutemart.quickcommerce.identity.api.IdentityAuthentication;
import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.IdentityRoleManagement;
import com.minutemart.quickcommerce.identity.api.InvitationIssued;
import com.minutemart.quickcommerce.identity.api.InviteIdentityCommand;
import com.minutemart.quickcommerce.identity.api.OtpChallengeIssued;
import com.minutemart.quickcommerce.identity.api.PasswordLoginCommand;
import com.minutemart.quickcommerce.identity.api.RefreshSessionCommand;
import com.minutemart.quickcommerce.identity.api.RequestOtpCommand;
import com.minutemart.quickcommerce.identity.api.RoleAssignmentView;
import com.minutemart.quickcommerce.identity.api.ScopeType;
import com.minutemart.quickcommerce.identity.api.VerifyOtpCommand;
import com.minutemart.quickcommerce.identity.application.AuthenticationApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class IdentityController {

    private final AuthenticationApplicationService authentication;
    private final IdentityAdministration administration;
    private final IdentityRoleManagement roleManagement;

    public IdentityController(
            AuthenticationApplicationService authentication,
            IdentityAdministration administration,
            IdentityRoleManagement roleManagement
    ) {
        this.authentication = authentication;
        this.administration = administration;
        this.roleManagement = roleManagement;
    }

    @PostMapping("/otp/challenges")
    ResponseEntity<ApiResponse<OtpChallengeIssued>> requestOtp(
            @Valid @RequestBody RequestOtpRequest request
    ) {
        OtpChallengeIssued result = authentication.requestOtp(new RequestOtpCommand(
                request.phoneNumber(),
                request.requestedRole(),
                request.requestedScopeType(),
                request.requestedScopeId(),
                request.clientApplication()
        ));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(success("AUTH_OTP_SENT", "OTP sent to " + result.maskedPhoneNumber() + " via SMS.", result));
    }

    @PostMapping("/otp/verify")
    ResponseEntity<ApiResponse<AuthenticationResult>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        AuthenticationResult result = authentication.verifyOtp(new VerifyOtpCommand(
                request.challengeId(),
                request.code(),
                request.requestedRole(),
                request.requestedScopeType(),
                request.requestedScopeId(),
                request.clientApplication()
        ));
        return ResponseEntity.ok(success("AUTHENTICATED", "Authentication successful.", result));
    }

    @PostMapping("/password/login")
    ResponseEntity<ApiResponse<AuthenticationResult>> login(
            @Valid @RequestBody PasswordLoginRequest request
    ) {
        AuthenticationResult result = authentication.loginWithPassword(new PasswordLoginCommand(
                request.phoneNumber(),
                request.password(),
                request.requestedRole(),
                request.requestedScopeType(),
                request.requestedScopeId(),
                request.clientApplication()
        ));
        return ResponseEntity.ok(success("AUTHENTICATED", "Authentication successful.", result));
    }

    @PostMapping("/sessions/refresh")
    ResponseEntity<ApiResponse<AuthenticationResult>> refresh(
            @Valid @RequestBody RefreshSessionRequest request
    ) {
        AuthenticationResult result = authentication.refresh(new RefreshSessionCommand(request.refreshToken()));
        return ResponseEntity.ok(success("AUTH_SESSION_REFRESHED", "Session refreshed.", result));
    }

    @PostMapping("/sessions/logout")
    ResponseEntity<Void> logout(Authentication current) {
        AuthenticatedIdentity identity = principal(current);
        authentication.logout(identity.identityId(), identity.sessionId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/switch-role")
    ResponseEntity<ApiResponse<AuthenticationResult>> switchRole(
            Authentication current,
            @Valid @RequestBody SwitchRoleRequest request
    ) {
        AuthenticatedIdentity identity = principal(current);
        AuthenticationResult result = authentication.switchRole(
                identity.identityId(),
                request.role(),
                request.scopeType(),
                request.scopeId(),
                request.clientApplication()
        );
        return ResponseEntity.ok(success("AUTH_ROLE_SWITCHED", "Role session created.", result));
    }

    @GetMapping("/me")
    ResponseEntity<ApiResponse<AuthenticatedIdentity>> me(Authentication current) {
        AuthenticatedIdentity identity = principal(current);
        return ResponseEntity.ok(success("AUTH_IDENTITY_RESOLVED", "Authenticated identity.", identity));
    }

    @PostMapping("/invitations")
    ResponseEntity<ApiResponse<InvitationIssued>> invite(
            Authentication current,
            @Valid @RequestBody InviteRequest request
    ) {
        AuthenticatedIdentity identity = requireAdmin(current);
        InvitationIssued result = administration.invite(new InviteIdentityCommand(
                request.phoneNumber(),
                request.role(),
                request.scopeType(),
                request.scopeId(),
                request.validity()
        ), identity.identityId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(success("AUTH_INVITATION_SENT", "Invitation sent.", result));
    }

    @PostMapping("/invitations/accept")
    ResponseEntity<ApiResponse<AuthenticationResult>> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request
    ) {
        AuthenticationResult result = administration.acceptInvitation(new AcceptInvitationCommand(
                request.invitationToken(), request.password()));
        return ResponseEntity.ok(success("AUTH_INVITATION_ACCEPTED", "Invitation accepted.", result));
    }

    @PostMapping("/roles")
    ResponseEntity<ApiResponse<RoleAssignmentView>> assignRole(
            Authentication current,
            @Valid @RequestBody AssignRoleRequest request
    ) {
        AuthenticatedIdentity identity = requireAdmin(current);
        RoleAssignmentView result = administration.assignRole(new AssignRoleCommand(
                request.identityId(),
                request.role(),
                request.scopeType(),
                request.scopeId(),
                request.activateImmediately()
        ), identity.identityId());
        return ResponseEntity.ok(success("AUTH_ROLE_ASSIGNED", "Role assignment updated.", result));
    }

    @PostMapping("/identities/{identityId}/block")
    ResponseEntity<Void> blockIdentity(Authentication current, @PathVariable UUID identityId) {
        AuthenticatedIdentity performer = requireAdmin(current);
        administration.blockIdentity(identityId, performer.identityId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/roles/activate")
    ResponseEntity<Void> activateRole(
            Authentication current,
            @Valid @RequestBody ActivateRoleRequest request
    ) {
        AuthenticatedIdentity performer = requireAdmin(current);
        roleManagement.activateRole(
                request.identityId(),
                request.role(),
                request.scopeType(),
                request.scopeId(),
                performer.identityId()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/roles/suspend")
    ResponseEntity<Void> suspendRole(
            Authentication current,
            @Valid @RequestBody SuspendRoleRequest request
    ) {
        AuthenticatedIdentity performer = requireAdmin(current);
        roleManagement.suspendRole(
                request.identityId(),
                request.role(),
                request.scopeType(),
                request.scopeId(),
                performer.identityId()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/change")
    ResponseEntity<Void> changePassword(
            Authentication current,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        AuthenticatedIdentity identity = principal(current);
        authentication.changePassword(new ChangePasswordCommand(
                identity.identityId(),
                request.currentPassword(),
                request.newPassword()
        ));
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedIdentity principal(Authentication current) {
        if (current == null || !(current.getPrincipal() instanceof AuthenticatedIdentity identity)) {
            throw new com.minutemart.quickcommerce.identity.domain.IdentityException(
                    com.minutemart.quickcommerce.identity.domain.IdentityError.SESSION_INVALID);
        }
        return identity;
    }

    private AuthenticatedIdentity requireAdmin(Authentication current) {
        AuthenticatedIdentity identity = principal(current);
        if (identity.role() != IdentityRole.ADMIN || identity.scopeType() != ScopeType.GLOBAL) {
            throw new com.minutemart.quickcommerce.identity.domain.IdentityException(
                    com.minutemart.quickcommerce.identity.domain.IdentityError.ROLE_NOT_ACTIVE);
        }
        return identity;
    }

    private static <T> ApiResponse<T> success(String code, String message, T data) {
        return new ApiResponse<>(code, message, data, Map.of());
    }

    public record RequestOtpRequest(
            @NotBlank @Size(max = 16) String phoneNumber,
            IdentityRole requestedRole,
            ScopeType requestedScopeType,
            @Size(max = 255) String requestedScopeId,
            @NotNull ClientApplication clientApplication
    ) {
    }

    public record VerifyOtpRequest(
            @NotNull UUID challengeId,
            @NotBlank @Size(min = 6, max = 6) String code,
            IdentityRole requestedRole,
            ScopeType requestedScopeType,
            @Size(max = 255) String requestedScopeId,
            @NotNull ClientApplication clientApplication
    ) {
    }

    public record PasswordLoginRequest(
            @NotBlank @Size(max = 16) String phoneNumber,
            @NotBlank @Size(min = 12, max = 128) String password,
            @NotNull IdentityRole requestedRole,
            @NotNull(message = "ScopeType cant be null") ScopeType requestedScopeType,
            @Size(max = 255) String requestedScopeId,
            @NotNull ClientApplication clientApplication
    ) {
    }

    public record RefreshSessionRequest(@NotBlank String refreshToken) {
    }

    public record SwitchRoleRequest(
            @NotNull IdentityRole role,
            @NotNull ScopeType scopeType,
            @Size(max = 255) String scopeId,
            @NotNull ClientApplication clientApplication
    ) {
    }

    public record InviteRequest(
            @NotBlank @Size(max = 16) String phoneNumber,
            @NotNull IdentityRole role,
            @NotNull ScopeType scopeType,
            @Size(max = 255) String scopeId,
            Duration validity
    ) {
    }

    public record AcceptInvitationRequest(
            @NotBlank String invitationToken,
            @NotBlank @Size(min = 12, max = 128) String password
    ) {
    }

    public record AssignRoleRequest(
            @NotNull UUID identityId,
            @NotNull IdentityRole role,
            @NotNull ScopeType scopeType,
            @Size(max = 255) String scopeId,
            boolean activateImmediately
    ) {
    }

    public record ActivateRoleRequest(
            @NotNull UUID identityId,
            @NotNull IdentityRole role,
            @NotNull ScopeType scopeType,
            @Size(max = 255) String scopeId
    ) {
    }

    public record SuspendRoleRequest(
            @NotNull UUID identityId,
            @NotNull IdentityRole role,
            @NotNull ScopeType scopeType,
            @Size(max = 255) String scopeId
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 12, max = 128) String newPassword
    ) {
    }
}
