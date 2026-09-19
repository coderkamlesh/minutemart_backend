package com.minutemart.quickcommerce.identity.application;

import com.minutemart.quickcommerce.identity.api.AuthenticatedIdentity;
import com.minutemart.quickcommerce.identity.api.AuthenticationResult;
import com.minutemart.quickcommerce.identity.api.ChangePasswordCommand;
import com.minutemart.quickcommerce.identity.api.ClientApplication;
import com.minutemart.quickcommerce.identity.api.IdentityAuthentication;
import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.OtpChallengeIssued;
import com.minutemart.quickcommerce.identity.api.PasswordLoginCommand;
import com.minutemart.quickcommerce.identity.api.RefreshSessionCommand;
import com.minutemart.quickcommerce.identity.api.RequestOtpCommand;
import com.minutemart.quickcommerce.identity.api.ScopeType;
import com.minutemart.quickcommerce.identity.api.VerifyOtpCommand;
import com.minutemart.quickcommerce.identity.domain.AuthSession;
import com.minutemart.quickcommerce.identity.domain.IdentityAccount;
import com.minutemart.quickcommerce.identity.domain.IdentityError;
import com.minutemart.quickcommerce.identity.domain.IdentityException;
import com.minutemart.quickcommerce.identity.domain.OtpChallenge;
import com.minutemart.quickcommerce.identity.domain.PhoneNumber;
import com.minutemart.quickcommerce.identity.domain.RoleAssignment;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

@Service
public class AuthenticationApplicationService implements IdentityAuthentication {

    private final IdentityAccountRepository identityAccounts;
    private final OtpChallengeRepository otpChallenges;
    private final AuthSessionRepository sessions;
    private final OtpDeliveryPort otpDelivery;
    private final TokenGenerator tokenGenerator;
    private final OtpCodeGenerator otpCodeGenerator;
    private final OtpFailureApplicationService otpFailures;
    private final TokenDigester tokenDigester;
    private final PasswordEncoder passwordEncoder;
    private final IdentityClock clock;
    private final IdentityAuthProperties properties;
    private final ApplicationEventPublisher events;

    public AuthenticationApplicationService(
            IdentityAccountRepository identityAccounts,
            OtpChallengeRepository otpChallenges,
            AuthSessionRepository sessions,
            OtpDeliveryPort otpDelivery,
            TokenGenerator tokenGenerator,
            OtpCodeGenerator otpCodeGenerator,
            OtpFailureApplicationService otpFailures,
            TokenDigester tokenDigester,
            PasswordEncoder passwordEncoder,
            IdentityClock clock,
            IdentityAuthProperties properties,
            ApplicationEventPublisher events
    ) {
        this.identityAccounts = identityAccounts;
        this.otpChallenges = otpChallenges;
        this.sessions = sessions;
        this.otpDelivery = otpDelivery;
        this.tokenGenerator = tokenGenerator;
        this.otpCodeGenerator = otpCodeGenerator;
        this.otpFailures = otpFailures;
        this.tokenDigester = tokenDigester;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.properties = properties;
        this.events = events;
    }

    @Transactional
    public OtpChallengeIssued requestOtp(RequestOtpCommand command) {
        PhoneNumber phoneNumber = PhoneNumber.of(command.phoneNumber());
        Instant now = clock.now();

        if (command.clientApplication() == null
                || (command.requestedRole() != null
                && !command.clientApplication().supports(command.requestedRole()))) {
            throw new IdentityException(IdentityError.INVALID_CLIENT_APPLICATION);
        }

        identityAccounts.findByPhoneNumber(phoneNumber).ifPresent(IdentityAccount::assertCanAuthenticate);
        otpChallenges.findLatestByPhoneNumber(phoneNumber).ifPresent(previous -> {
            if (previous.createdAt().plus(properties.getOtpResendCooldown()).isAfter(now)) {
                throw new IdentityException(IdentityError.OTP_RESEND_TOO_SOON);
            }
        });

        String code = otpCodeGenerator.generate();
        OtpChallenge challenge = OtpChallenge.issue(
                UUID.randomUUID(),
                phoneNumber,
                com.minutemart.quickcommerce.identity.domain.ChallengePurpose.LOGIN,
                passwordEncoder.encode(code),
                now,
                now.plus(properties.getOtpValidity()),
                properties.getOtpMaxAttempts()
        );
        otpChallenges.save(challenge);
        otpDelivery.send(phoneNumber, code, challenge.expiresAt());

        return new OtpChallengeIssued(
                challenge.challengeId(),
                phoneNumber.masked(),
                challenge.expiresAt(),
                properties.isExposeDevelopmentSecrets() ? code : null
        );
    }

    @Transactional
    public AuthenticationResult verifyOtp(VerifyOtpCommand command) {
        Instant now = clock.now();
        OtpChallenge challenge = otpChallenges.findById(command.challengeId())
                .orElseThrow(() -> new IdentityException(IdentityError.OTP_CHALLENGE_NOT_FOUND));
        challenge.ensureVerifiable(now);

        if (!passwordEncoder.matches(command.code(), challenge.codeHash())) {
            IdentityError error = otpFailures.recordInvalidAttempt(command.challengeId());
            throw new IdentityException(error);
        }

        if (otpChallenges.markVerified(command.challengeId(), now, now) != 1) {
            throw new IdentityException(IdentityError.OTP_INVALID);
        }

        IdentityAccount account = identityAccounts.findByPhoneNumber(challenge.phoneNumber())
                .orElseGet(() -> registerConsumer(challenge.phoneNumber(), now));
        account.assertCanAuthenticate();
        events.publishEvent(new com.minutemart.quickcommerce.identity.events.ContactVerified(
                account.identityId(), "PHONE", now));
        RoleAssignment role = selectRole(account, command.requestedRole(),
                command.requestedScopeType(), command.requestedScopeId());
        return issueSession(account, role, command.clientApplication(), now);
    }

    @Transactional
    public AuthenticationResult loginWithPassword(PasswordLoginCommand command) {
        PhoneNumber phoneNumber = PhoneNumber.of(command.phoneNumber());
        IdentityAccount account = identityAccounts.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IdentityException(IdentityError.INVALID_CREDENTIALS));
        account.assertCanAuthenticate();
        if (!account.hasPassword() || !passwordEncoder.matches(command.password(), account.passwordHash())) {
            throw new IdentityException(IdentityError.INVALID_CREDENTIALS);
        }

        RoleAssignment role = selectRole(account, command.requestedRole(),
                command.requestedScopeType(), command.requestedScopeId());
        if (command.clientApplication() == null || !command.clientApplication().supports(role.role())) {
            throw new IdentityException(IdentityError.INVALID_CLIENT_APPLICATION);
        }
        return issueSession(account, role, command.clientApplication(), clock.now());
    }

    @Transactional
    public AuthenticationResult refresh(RefreshSessionCommand command) {
        Instant now = clock.now();
        AuthSession current = sessions.findByRefreshTokenHashForUpdate(tokenDigester.digest(command.refreshToken()))
                .orElseThrow(() -> new IdentityException(IdentityError.REFRESH_TOKEN_INVALID));
        current.ensureRefreshable(now);

        IdentityAccount account = identityAccounts.findById(current.identityId())
                .orElseThrow(() -> new IdentityException(IdentityError.IDENTITY_NOT_FOUND));
        account.assertCanAuthenticate();
        RoleAssignment role = account.activeRole(current.role(), current.scopeType(), current.scopeId())
                .orElseThrow(() -> new IdentityException(IdentityError.ROLE_NOT_ACTIVE));

        if (sessions.consumeRefreshToken(current.sessionId(), now, now) != 1) {
            throw new IdentityException(IdentityError.REFRESH_TOKEN_INVALID);
        }
        return issueSession(account, role, current.clientApplication(), now);
    }

    @Transactional
    public AuthenticatedIdentity authenticateAccessToken(String accessToken) {
        Instant now = clock.now();
        AuthSession session = sessions.findByAccessTokenHash(tokenDigester.digest(accessToken))
                .orElseThrow(() -> new IdentityException(IdentityError.SESSION_INVALID));
        session.authenticate(now);

        IdentityAccount account = identityAccounts.findById(session.identityId())
                .orElseThrow(() -> new IdentityException(IdentityError.IDENTITY_NOT_FOUND));
        account.assertCanAuthenticate();
        account.activeRole(session.role(), session.scopeType(), session.scopeId())
                .orElseThrow(() -> new IdentityException(IdentityError.ROLE_NOT_ACTIVE));
        sessions.save(session);

        return new AuthenticatedIdentity(
                session.identityId(),
                session.sessionId(),
                session.role(),
                session.scopeType(),
                session.scopeId(),
                session.clientApplication()
        );
    }

    @Transactional
    public AuthenticationResult switchRole(
            UUID identityId,
            IdentityRole role,
            ScopeType scopeType,
            String scopeId,
            ClientApplication clientApplication
    ) {
        return authenticateIdentity(identityId, role, scopeType, scopeId, clientApplication);
    }

    @Transactional
    public AuthenticationResult authenticateIdentity(
            UUID identityId,
            IdentityRole role,
            ScopeType scopeType,
            String scopeId,
            ClientApplication clientApplication
    ) {
        IdentityAccount account = identityAccounts.findById(identityId)
                .orElseThrow(() -> new IdentityException(IdentityError.IDENTITY_NOT_FOUND));
        account.assertCanAuthenticate();
        RoleAssignment assignment = account.activeRole(role, scopeType, scopeId)
                .orElseThrow(() -> new IdentityException(IdentityError.ROLE_NOT_ACTIVE));
        if (clientApplication == null || !clientApplication.supports(role)) {
            throw new IdentityException(IdentityError.INVALID_CLIENT_APPLICATION);
        }
        return issueSession(account, assignment, clientApplication, clock.now());
    }

    @Transactional
    public void logout(UUID identityId, UUID sessionId) {
        AuthSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new IdentityException(IdentityError.SESSION_INVALID));
        if (!session.identityId().equals(identityId)) {
            throw new IdentityException(IdentityError.SESSION_INVALID);
        }
        session.revoke(clock.now());
        sessions.save(session);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        Instant now = clock.now();
        IdentityAccount account = identityAccounts.findById(command.identityId())
                .orElseThrow(() -> new IdentityException(IdentityError.IDENTITY_NOT_FOUND));
        account.assertCanAuthenticate();

        if (!account.hasPassword() || !passwordEncoder.matches(command.currentPassword(), account.passwordHash())) {
            throw new IdentityException(IdentityError.INVALID_CREDENTIALS);
        }

        if (command.newPassword().length() < 12) {
            throw new IdentityException(IdentityError.INVALID_CREDENTIALS);
        }

        account.setPasswordHash(passwordEncoder.encode(command.newPassword()), now);
        identityAccounts.save(account);
        sessions.revokeAllForIdentity(account.identityId(), now);
    }

    private IdentityAccount registerConsumer(PhoneNumber phoneNumber, Instant now) {
        IdentityAccount account = IdentityAccount.register(UUID.randomUUID(), phoneNumber, now);
        account.assignRole(UUID.randomUUID(), IdentityRole.CONSUMER,
                ScopeType.GLOBAL, null, true, now);
        IdentityAccount saved = identityAccounts.save(account);
        events.publishEvent(new com.minutemart.quickcommerce.identity.events.IdentityCreated(
                saved.identityId(), now));
        events.publishEvent(new com.minutemart.quickcommerce.identity.events.RoleAssigned(
                saved.findAssignment(IdentityRole.CONSUMER, ScopeType.GLOBAL, null).orElseThrow().assignmentId(),
                saved.identityId(), IdentityRole.CONSUMER.name(), ScopeType.GLOBAL.name(),
                null, "ACTIVE", now));
        events.publishEvent(new com.minutemart.quickcommerce.identity.events.RoleActivated(
                saved.findAssignment(IdentityRole.CONSUMER, ScopeType.GLOBAL, null).orElseThrow().assignmentId(),
                saved.identityId(), IdentityRole.CONSUMER.name(), ScopeType.GLOBAL.name(),
                null, now));
        return saved;
    }

    private RoleAssignment selectRole(
            IdentityAccount account,
            IdentityRole requestedRole,
            ScopeType requestedScopeType,
            String requestedScopeId
    ) {
        if (requestedRole != null) {
            if (requestedScopeType != null) {
                return account.activeRole(requestedRole, requestedScopeType, requestedScopeId)
                        .orElseThrow(() -> new IdentityException(IdentityError.ROLE_NOT_ACTIVE));
            }
            var matchingRoles = account.activeRoleAssignments().stream()
                    .filter(role -> role.role() == requestedRole)
                    .sorted(Comparator.comparing(RoleAssignment::grantedAt))
                    .toList();
            if (matchingRoles.size() == 1) {
                return matchingRoles.getFirst();
            }
            if (matchingRoles.isEmpty()) {
                throw new IdentityException(IdentityError.ROLE_NOT_ACTIVE);
            }
            throw new IdentityException(IdentityError.ROLE_SELECTION_REQUIRED);
        }

        var activeRoles = account.activeRoleAssignments();
        if (activeRoles.size() == 1) {
            return activeRoles.iterator().next();
        }
        if (activeRoles.isEmpty()) {
            throw new IdentityException(IdentityError.ROLE_NOT_ACTIVE);
        }
        throw new IdentityException(IdentityError.ROLE_SELECTION_REQUIRED);
    }

    private AuthenticationResult issueSession(
            IdentityAccount account,
            RoleAssignment role,
            ClientApplication clientApplication,
            Instant now
    ) {
        String accessToken = tokenGenerator.generate();
        String refreshToken = tokenGenerator.generate();
        AuthSession session = AuthSession.issue(
                UUID.randomUUID(),
                account.identityId(),
                tokenDigester.digest(accessToken),
                tokenDigester.digest(refreshToken),
                role.role(),
                role.scopeType(),
                role.scopeId(),
                clientApplication,
                now,
                now.plus(properties.getAccessTokenValidity()),
                now.plus(properties.getRefreshTokenValidity())
        );
        sessions.save(session);
        return new AuthenticationResult(
                account.identityId(),
                session.sessionId(),
                role.role(),
                role.scopeType(),
                role.scopeId(),
                clientApplication,
                "Bearer",
                accessToken,
                session.accessExpiresAt(),
                refreshToken,
                session.refreshExpiresAt()
        );
    }
}
