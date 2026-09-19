package com.minutemart.quickcommerce.identity.web;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.minutemart.quickcommerce.identity.api.AuthenticatedIdentity;
import com.minutemart.quickcommerce.identity.api.AuthenticationResult;
import com.minutemart.quickcommerce.identity.api.ClientApplication;
import com.minutemart.quickcommerce.identity.api.IdentityAdministration;
import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.IdentityRoleManagement;
import com.minutemart.quickcommerce.identity.api.OtpChallengeIssued;
import com.minutemart.quickcommerce.identity.api.ScopeType;
import com.minutemart.quickcommerce.identity.application.AuthenticationApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IdentityControllerTest {

    @Mock
    private AuthenticationApplicationService authentication;
    @Mock
    private IdentityAdministration administration;
    @Mock
    private IdentityRoleManagement roleManagement;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        IdentityController controller = new IdentityController(authentication, administration, roleManagement);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IdentityExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /otp/challenges should return 202 Accepted on valid request")
    void requestOtpShouldReturnAccepted() throws Exception {
        UUID challengeId = UUID.randomUUID();
        when(authentication.requestOtp(any())).thenReturn(
                new OtpChallengeIssued(challengeId, "+91******3210", Instant.now().plusSeconds(300), null)
        );

        var request = new IdentityController.RequestOtpRequest(
                "+919876543210",
                IdentityRole.CONSUMER,
                ScopeType.GLOBAL,
                null,
                ClientApplication.CONSUMER_APP
        );

        mockMvc.perform(post("/api/v1/auth/otp/challenges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("AUTH_OTP_SENT"))
                .andExpect(jsonPath("$.data.challengeId").value(challengeId.toString()));
    }

    @Test
    @DisplayName("POST /otp/challenges should return 400 Bad Request on blank phone")
    void requestOtpShouldValidatePhoneNumber() throws Exception {
        var request = new IdentityController.RequestOtpRequest(
                "",
                IdentityRole.CONSUMER,
                ScopeType.GLOBAL,
                null,
                ClientApplication.CONSUMER_APP
        );

        mockMvc.perform(post("/api/v1/auth/otp/challenges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_REQUEST"));
    }

    @Test
    @DisplayName("POST /otp/verify should return 200 OK on valid verification")
    void verifyOtpShouldReturnOk() throws Exception {
        UUID challengeId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(authentication.verifyOtp(any())).thenReturn(new AuthenticationResult(
                identityId, sessionId, IdentityRole.CONSUMER, ScopeType.GLOBAL, null,
                ClientApplication.CONSUMER_APP, "Bearer", "access-token", Instant.now().plusSeconds(900),
                "refresh-token", Instant.now().plusSeconds(86400)
        ));

        var request = new IdentityController.VerifyOtpRequest(
                challengeId,
                "123456",
                IdentityRole.CONSUMER,
                ScopeType.GLOBAL,
                null,
                ClientApplication.CONSUMER_APP
        );

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("POST /roles/activate should call roleManagement when authenticated as admin")
    void activateRoleShouldSucceedForAdmin() throws Exception {
        UUID adminId = UUID.randomUUID();
        AuthenticatedIdentity admin = new AuthenticatedIdentity(
                adminId, UUID.randomUUID(), IdentityRole.ADMIN, ScopeType.GLOBAL, "", ClientApplication.ADMIN_PORTAL
        );
        var auth = UsernamePasswordAuthenticationToken.authenticated(admin, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID targetIdentityId = UUID.randomUUID();
        var request = new IdentityController.ActivateRoleRequest(
                targetIdentityId, IdentityRole.STORE_MANAGER, ScopeType.STORE, "store-1"
        );

        mockMvc.perform(post("/api/v1/auth/roles/activate")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(roleManagement).activateRole(targetIdentityId, IdentityRole.STORE_MANAGER, ScopeType.STORE, "store-1", adminId);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /password/change should call authentication.changePassword for authenticated user")
    void changePasswordShouldSucceed() throws Exception {
        UUID identityId = UUID.randomUUID();
        AuthenticatedIdentity user = new AuthenticatedIdentity(
                identityId, UUID.randomUUID(), IdentityRole.ADMIN, ScopeType.GLOBAL, "", ClientApplication.ADMIN_PORTAL
        );
        var auth = UsernamePasswordAuthenticationToken.authenticated(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        var request = new IdentityController.ChangePasswordRequest("OldPassword123!", "NewPassword1234!");

        mockMvc.perform(post("/api/v1/auth/password/change")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authentication).changePassword(any());
        SecurityContextHolder.clearContext();
    }
}
