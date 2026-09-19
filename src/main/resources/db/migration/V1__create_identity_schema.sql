create table identity_account (
    identity_id uuid primary key,
    phone_number varchar(16) not null unique,
    status varchar(32) not null,
    password_hash varchar(100),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint ck_identity_account_status
        check (status in ('ACTIVE', 'BLOCKED', 'DELETED'))
);

create table identity_role_assignment (
    assignment_id uuid primary key,
    identity_id uuid not null references identity_account(identity_id),
    role varchar(32) not null,
    scope_type varchar(32) not null,
    scope_id varchar(255) not null,
    status varchar(32) not null,
    granted_at timestamp with time zone not null,
    activated_at timestamp with time zone,
    suspended_at timestamp with time zone,
    constraint uq_identity_role_scope
        unique (identity_id, role, scope_type, scope_id),
    constraint ck_identity_role_status
        check (status in ('PENDING', 'ACTIVE', 'SUSPENDED', 'REJECTED')),
    constraint ck_identity_role_scope_id
        check (
            (scope_type = 'GLOBAL' and scope_id = '')
            or (scope_type <> 'GLOBAL' and scope_id <> '')
        )
);

create index ix_identity_role_assignment_identity
    on identity_role_assignment(identity_id);

create table identity_otp_challenge (
    challenge_id uuid primary key,
    phone_number varchar(16) not null,
    purpose varchar(32) not null,
    code_hash varchar(100) not null,
    created_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    max_attempts integer not null,
    status varchar(32) not null,
    attempts integer not null,
    verified_at timestamp with time zone,
    constraint ck_identity_otp_status
        check (status in ('CREATED', 'VERIFIED', 'EXPIRED', 'LOCKED')),
    constraint ck_identity_otp_attempts
        check (attempts >= 0 and max_attempts > 0)
);

create index ix_identity_otp_phone_created
    on identity_otp_challenge(phone_number, created_at desc);

create table identity_auth_session (
    session_id uuid primary key,
    identity_id uuid not null references identity_account(identity_id),
    access_token_hash varchar(128) not null unique,
    refresh_token_hash varchar(128) not null unique,
    role varchar(32) not null,
    scope_type varchar(32) not null,
    scope_id varchar(255) not null,
    client_application varchar(32) not null,
    created_at timestamp with time zone not null,
    access_expires_at timestamp with time zone not null,
    refresh_expires_at timestamp with time zone not null,
    revoked_at timestamp with time zone,
    last_used_at timestamp with time zone,
    constraint ck_identity_session_scope_id
        check (
            (scope_type = 'GLOBAL' and scope_id = '')
            or (scope_type <> 'GLOBAL' and scope_id <> '')
        )
);

create index ix_identity_session_identity
    on identity_auth_session(identity_id);

create index ix_identity_session_refresh_validity
    on identity_auth_session(refresh_token_hash, revoked_at, refresh_expires_at);

create table identity_invitation (
    invitation_id uuid primary key,
    phone_number varchar(16) not null,
    role varchar(32) not null,
    scope_type varchar(32) not null,
    scope_id varchar(255) not null,
    token_hash varchar(128) not null unique,
    invited_by uuid not null references identity_account(identity_id),
    created_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    status varchar(32) not null,
    accepted_at timestamp with time zone,
    constraint ck_identity_invitation_status
        check (status in ('CREATED', 'ACCEPTED', 'EXPIRED', 'REVOKED')),
    constraint ck_identity_invitation_scope_id
        check (
            (scope_type = 'GLOBAL' and scope_id = '')
            or (scope_type <> 'GLOBAL' and scope_id <> '')
        )
);

create index ix_identity_invitation_phone
    on identity_invitation(phone_number, created_at desc);
