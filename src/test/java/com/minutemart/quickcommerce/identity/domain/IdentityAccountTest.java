package com.minutemart.quickcommerce.identity.domain;

import com.minutemart.quickcommerce.identity.api.IdentityRole;
import com.minutemart.quickcommerce.identity.api.ScopeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityAccountTest {

    private final Instant now = Instant.parse("2026-09-20T00:00:00Z");
    private final PhoneNumber phone = PhoneNumber.of("+919876543210");

    @Test
    @DisplayName("register should create active account with no roles and no password")
    void registerShouldCreateActiveAccount() {
        UUID identityId = UUID.randomUUID();
        IdentityAccount account = IdentityAccount.register(identityId, phone, now);

        assertEquals(identityId, account.identityId());
        assertEquals(phone, account.phoneNumber());
        assertEquals(IdentityStatus.ACTIVE, account.status());
        assertFalse(account.hasPassword());
        assertTrue(account.roleAssignments().isEmpty());
    }

    @Test
    @DisplayName("assignRole should add active role assignment when activated immediately")
    void assignRoleShouldAddActiveRole() {
        IdentityAccount account = IdentityAccount.register(UUID.randomUUID(), phone, now);
        UUID assignmentId = UUID.randomUUID();

        RoleAssignment assignment = account.assignRole(
                assignmentId, IdentityRole.CONSUMER, ScopeType.GLOBAL, null, true, now
        );

        assertNotNull(assignment);
        assertEquals(assignmentId, assignment.assignmentId());
        assertEquals(RoleAssignmentStatus.ACTIVE, assignment.status());
        assertTrue(account.hasActiveRole(IdentityRole.CONSUMER));
        assertTrue(account.activeRole(IdentityRole.CONSUMER, ScopeType.GLOBAL, null).isPresent());
    }

    @Test
    @DisplayName("assignRole should throw when duplicate role assignment is attempted")
    void assignRoleDuplicateShouldThrow() {
        IdentityAccount account = IdentityAccount.register(UUID.randomUUID(), phone, now);
        account.assignRole(UUID.randomUUID(), IdentityRole.CONSUMER, ScopeType.GLOBAL, null, true, now);

        IdentityException ex = assertThrows(IdentityException.class, () ->
                account.assignRole(UUID.randomUUID(), IdentityRole.CONSUMER, ScopeType.GLOBAL, null, true, now)
        );

        assertEquals(IdentityError.ROLE_ALREADY_ASSIGNED, ex.error());
    }

    @Test
    @DisplayName("assignRole should reject invalid scope type for role")
    void assignRoleShouldRejectInvalidScope() {
        IdentityAccount account = IdentityAccount.register(UUID.randomUUID(), phone, now);

        IdentityException ex = assertThrows(IdentityException.class, () ->
                account.assignRole(UUID.randomUUID(), IdentityRole.STORE_MANAGER, ScopeType.GLOBAL, null, true, now)
        );

        assertEquals(IdentityError.INVALID_ROLE_SCOPE, ex.error());
    }

    @Test
    @DisplayName("activateRole and suspendRole should transition role status correctly")
    void activateAndSuspendRoleShouldTransitionStatus() {
        IdentityAccount account = IdentityAccount.register(UUID.randomUUID(), phone, now);
        account.assignRole(UUID.randomUUID(), IdentityRole.STORE_MANAGER, ScopeType.STORE, "store-1", false, now);

        assertFalse(account.hasActiveRole(IdentityRole.STORE_MANAGER));

        account.activateRole(IdentityRole.STORE_MANAGER, ScopeType.STORE, "store-1", now.plusSeconds(10));
        assertTrue(account.hasActiveRole(IdentityRole.STORE_MANAGER));

        account.suspendRole(IdentityRole.STORE_MANAGER, ScopeType.STORE, "store-1", now.plusSeconds(20));
        assertFalse(account.hasActiveRole(IdentityRole.STORE_MANAGER));
    }

    @Test
    @DisplayName("block should change account status and assertCanAuthenticate should throw")
    void blockShouldPreventAuthentication() {
        IdentityAccount account = IdentityAccount.register(UUID.randomUUID(), phone, now);
        account.block(now);

        assertEquals(IdentityStatus.BLOCKED, account.status());

        IdentityException ex = assertThrows(IdentityException.class, account::assertCanAuthenticate);
        assertEquals(IdentityError.IDENTITY_BLOCKED, ex.error());
    }

    @Test
    @DisplayName("setPasswordHash should update password hash on account")
    void setPasswordHashShouldUpdatePassword() {
        IdentityAccount account = IdentityAccount.register(UUID.randomUUID(), phone, now);
        account.setPasswordHash("hashed_password_value", now);

        assertTrue(account.hasPassword());
        assertEquals("hashed_password_value", account.passwordHash());
    }
}
