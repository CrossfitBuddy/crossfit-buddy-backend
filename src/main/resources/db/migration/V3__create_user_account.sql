create table user_account (
    id uuid primary key default gen_random_uuid(),
    email varchar(320) not null unique,
    password_hash varchar(255) not null,
    active_role user_role constraint fk_user_active_role references roles(name),
    enabled boolean not null default true,
    deleted boolean not null default false,
    token_version bigint not null default 0 check (token_version >= 0),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_user_email_normalized check (email = lower(btrim(email)))
);

create unique index ux_user_account_email_normalized on user_account (lower(btrim(email)));
