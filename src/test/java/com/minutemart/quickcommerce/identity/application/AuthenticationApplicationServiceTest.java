package com.minutemart.quickcommerce.identity.application;

import com.minutemart.quickcommerce.identity.api.ChangePasswordCommand;
import com.minutemart.quickcommerce.identity.api.ClientApplication;
import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.OtpChallengeIssued;
import com.minutemart.quickcommerce.identity.api.PasswordLoginCommand;
import com.minutemart.quickcommerce.identity.api.RequestOtpCommand;
import com.minutemart.quickcommerce.identity.api.ScopeType;
import com.minutemart.quickcommerce.identity.domain.AuthSession;
import com.minutemart.quickcommerce.identity.domain.ChallengePurpose;
import com.minutemart.quickcommerce.identity.domain.IdentityAccount;
import com.minutemart.quickcommerce.identity.domain.IdentityError;
import com.minutemart.quickcommerce.identity.domain.IdentityException;
import com.minutemart.quickcommerce.identity.domain.OtpChallenge;
import com.minutemart.quickcommerce.identity.domain.PhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationApplicationServiceTest {

    @Mock
    private IdentityAccountRepository identityAccounts;
    @Mock
    private OtpChallengeRepository otpChallenges;
    @Mock
    private AuthSessionRepository sessions;
    @Mock
    private OtpDeliveryPort otpDelivery;
    @Mock
    private TokenGenerator tokenGenerator;
    @Mock
    private OtpCodeGenerator otpCodeGenerator;
    @Mock
    private OtpFailureApplicationService otpFailures;
    @Mock
    private TokenDigester tokenDigester;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private IdentityClock clock;
    @Mock
    private ApplicationEventPublisher events;

    private IdentityAuthProperties properties;
    private AuthenticationApplicationService service;

    private final Instant now = Instant.parse("2026-09-20T10:00:00Z");
    private final PhoneNumber phone = PhoneNumber.of("+919876543210");

    @BeforeEach
    void setUp() {
        properties = new IdentityAuthProperties();
        properties.setOtpResendCooldown(Duration.ofSeconds(30));
        properties.setOtpValidity(Duration.ofMinutes(5));
        properties.setOtpMaxAttempts(5);
        properties.setAccessTokenValidity(Duration.ofMinutes(15));
        properties.setRefreshTokenValidity(Duration.ofDays(30));

        lenient().when(clock.now()).thenReturn(now);

        service = new AuthenticationApplicationService(
                identityAccounts,
                otpChallenges,
                sessions,
                otpDelivery,
                tokenGenerator,
                otpCodeGenerator,
                otpFailures,
                tokenDigester,
                passwordEncoder,
                clock,
                properties,
                events
        );
    }

    @Test
    @DisplayName("requestOtp should issue challenge and deliver code")
    void requestOtpShouldIssueChallenge() {
        RequestOtpCommand command = new RequestOtpCommand(
                phone.value(),
                IdentityRole.CONSUMER,
                ScopeType.GLOBAL,
                null,
                ClientApplication.CONSUMER_APP
        );

        when(identityAccounts.findByPhoneNumber(phone)).thenReturn(Optional.empty());
        when(otpChallenges.findLatestByPhoneNumber(phone)).thenReturn(Optional.empty());
        when(otpCodeGenerator.generate()).thenReturn("123456");
        when(passwordEncoder.encode("123456")).thenReturn("hashed-otp");

        OtpChallengeIssued result = service.requestOtp(command);

        assertNotNull(result);
        assertEquals(phone.masked(), result.maskedPhoneNumber());
        verify(otpChallenges).save(any(OtpChallenge.class));
        verify(otpDelivery).send(eq(phone), eq("123456"), any(Instant.class));
    }

    @Test
    @DisplayName("requestOtp should reject when resend cooldown has not elapsed")
    void requestOtpShouldRejectWhenCooldownActive() {
        RequestOtpCommand command = new RequestOtpCommand(
                phone.value(),
                IdentityRole.CONSUMER,
                ScopeType.GLOBAL,
                null,
                ClientApplication.CONSUMER_APP
        );

        OtpChallenge previous = OtpChallenge.issue(
                UUID.randomUUID(), phone, ChallengePurpose.LOGIN, "hash",
                now.minusSeconds(10), now.plusSeconds(290), 5
        );
        when(identityAccounts.findByPhoneNumber(phone)).thenReturn(Optional.empty());
        when(otpChallenges.findLatestByPhoneNumber(phone)).thenReturn(Optional.of(previous));

        IdentityException ex = assertThrows(IdentityException.class, () -> service.requestOtp(command));
        assertEquals(IdentityError.OTP_RESEND_TOO_SOON, ex.error());
    }

    @Test
    @DisplayName("loginWithPassword should reject invalid credentials")
    void loginWithPasswordShouldRejectWrongPassword() {
        PasswordLoginCommand command = new PasswordLoginCommand(
                phone.value(),
                "WrongPassword123",
                IdentityRole.ADMIN,
                ScopeType.GLOBAL,
                null,
                ClientApplication.ADMIN_PORTAL
        );

        IdentityAccount account = IdentityAccount.register(UUID.randomUUID(), phone, now);
        account.setPasswordHash("encoded-pass", now);

        when(identityAccounts.findByPhoneNumber(phone)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("WrongPassword123", "encoded-pass")).thenReturn(false);

        IdentityException ex = assertThrows(IdentityException.class, () -> service.loginWithPassword(command));
        assertEquals(IdentityError.INVALID_CREDENTIALS, ex.error());
    }

    @Test
    @DisplayName("loginWithPassword should authenticate admin and issue session")
    void loginWithPasswordShouldAuthenticateAdmin() {
        PasswordLoginCommand command = new PasswordLoginCommand(
                phone.value(),
                "Admin@MinuteMart2026",
                IdentityRole.ADMIN,
                ScopeType.GLOBAL,
                null,
                ClientApplication.ADMIN_PORTAL
        );

        UUID adminId = UUID.randomUUID();
        IdentityAccount account = IdentityAccount.register(adminId, phone, now);
        account.setPasswordHash("encoded-admin-pass", now);
        account.assignRole(UUID.randomUUID(), IdentityRole.ADMIN, ScopeType.GLOBAL, null, true, now);

        when(identityAccounts.findByPhoneNumber(phone)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Admin@MinuteMart2026", "encoded-admin-pass")).thenReturn(true);
        when(tokenGenerator.generate()).thenReturn("token-1", "token-2");
        when(tokenDigester.digest(any())).thenReturn("hashed-token");

        AuthenticationResult result = service.loginWithPassword(command);

        assertNotNull(result);
        assertEquals(adminId, result.identityId());
        assertEquals(IdentityRole.ADMIN, result.activeRole());
        assertEquals(ScopeType.GLOBAL, result.activeScopeType());
        assertEquals("token-1", result.accessToken());
        assertEquals("token-2", result.refreshToken());
        verify(sessions).save(any());
    }

    @Test
    @DisplayName("changePassword should update password and revoke other sessions")
    void changePasswordShouldUpdatePasswordAndRevokeSessions() {
        UUID identityId = UUID.randomUUID();
        IdentityAccount account = IdentityAccount.register(identityId, phone, now);
        account.setPasswordHash("encoded-old-pass", now);

        when(identityAccounts.findById(identityId)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("OldPassword123!", "encoded-old-pass")).thenReturn(true);
        when(passwordEncoder.encode("NewSecretPassword123!")).thenReturn("encoded-new-pass");

        service.changePassword(new ChangePasswordCommand(identityId, "OldPassword123!", "NewSecretPassword123!"));

        ArgumentCaptor<IdentityAccount> captor = ArgumentCaptor.forClass(IdentityAccount.class);
        verify(identityAccounts).save(captor.capture());
        assertEquals("encoded-new-pass", captor.getValue().passwordHash());
        verify(sessions).revokeAllForIdentity(identityId, now);
    }

    @Test
    @DisplayName("changePassword should reject when new password is too short")
    void changePasswordShouldRejectShortNewPassword() {
        UUID identityId = UUID.randomUUID();
        IdentityAccount account = IdentityAccount.register(identityId, phone, now);
        account.setPasswordHash("encoded-old-pass", now);

        when(identityAccounts.findById(identityId)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("OldPassword123!", "encoded-old-pass")).thenReturn(true);

        IdentityException ex = assertThrows(IdentityException.class, () ->
                service.changePassword(new ChangePasswordCommand(identityId, "OldPassword123!", "short"))
        );
        assertEquals(IdentityError.INVALID_CREDENTIALS, ex.error());
    }

    @Test
    @DisplayName("logout should revoke session")
    void logoutShouldRevokeSession() {
        UUID identityId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AuthSession session = AuthSession.issue(
                sessionId, identityId, "access-hash", "refresh-hash",
                IdentityRole.CONSUMER, ScopeType.GLOBAL, null,
                ClientApplication.CONSUMER_APP, now, now.plusSeconds(900), now.plusSeconds(86400)
        );

        when(sessions.findById(sessionId)).thenReturn(Optional.of(session));

        service.logout(identityId, sessionId);

        verify(sessions).save(session);
    }
}
