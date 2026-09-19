# ADR-0004: Identity Persistence and Delivery Boundaries

## Status

Accepted

## Context

The identity module needs durable accounts, role assignments, OTP challenges, invitations, and opaque sessions. It also needs to deliver OTPs and invitations through external providers. Authentication state must not depend on Hibernate auto-creating production tables or on a silent no-op delivery adapter.

## Decision

- Identity owns the tables `identity_account`, `identity_role_assignment`, `identity_otp_challenge`, `identity_auth_session`, and `identity_invitation`.
- Schema changes use Flyway migrations. JPA is configured with `ddl-auto: validate` so mapping drift fails startup rather than silently changing production schema.
- Scope IDs are normalized to an empty string for global roles because scoped role and session tables require a non-null `scope_id`.
- Access and refresh tokens are opaque random values. Only SHA-256 digests are persisted.
- OTP codes are generated randomly and stored only as BCrypt hashes.
- Development delivery is available only through an explicit development mode/profile. A production delivery adapter must be supplied before selecting a non-development delivery mode.
- External delivery is currently an adapter boundary. Before production use, delivery must be moved to an after-commit/outbox workflow with challenge/invitation IDs as idempotency keys; raw secrets must not enter the general integration event stream.

## Alternatives considered

### Hibernate schema generation

Rejected for production because it does not provide a reviewable, versioned migration history.

### JWT access tokens

Rejected for the first implementation because immediate revocation after account or role suspension is a hard requirement. Database-backed opaque sessions make revocation explicit and keep token contents out of clients.

### Silent development delivery in every environment

Rejected because returning success while dropping OTPs or invitations hides an operational failure.

## Consequences

- Identity state is durable and independently owned.
- Every deployment needs Flyway access to the identity schema.
- Opaque-token lookup adds a database read per authenticated request; this can be optimized later only with a revocation-aware cache.
- Production must add a real SMS/invitation provider adapter and an after-commit delivery workflow.
- Database unique constraints and atomic state transitions remain necessary for concurrent OTP, refresh, and role operations.

## Enforcement

- `src/main/resources/db/migration/V1__create_identity_schema.sql`
- `spring.jpa.hibernate.ddl-auto=validate`
- Identity entities use only identity-owned table names.
- Token and OTP fields are digests/hashes, never raw secrets.
- Delivery mode is configured under `identity.auth.delivery-mode`.
