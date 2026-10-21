create table password_reset_code (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references user_account(id) on delete cascade,
    code_hash varchar(255) not null,
    failed_attempts integer not null default 0 check (failed_attempts between 0 and 5),
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    consumed_at timestamptz,
    check (expires_at > created_at and expires_at <= created_at + interval '15 minutes')
);

create index idx_password_reset_user on password_reset_code(user_id, created_at desc);

create index idx_password_reset_expiration on password_reset_code(expires_at);
