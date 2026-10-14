create table roles (
    id uuid primary key default gen_random_uuid(),
    name user_role not null unique,
    description text not null
);
insert into roles (name, description) values
    ('ATHLETE', 'Выполнение тренировок и сохранение собственных результатов'),
    ('TRAINER', 'Подготовка тренировок и сопровождение атлетов'),
    ('ADMIN', 'Администрирование системы');
alter table user_roles add constraint fk_user_roles_role foreign key (role) references roles(name);
alter table app_user add constraint fk_user_active_role foreign key (active_role) references roles(name);

alter table app_user add column first_name varchar(100);
alter table app_user add column last_name varchar(100);
alter table app_user alter column display_name type varchar(201);
alter table app_user add column deleted boolean not null default false;
alter table app_user add column token_version bigint not null default 0 check (token_version >= 0);
alter table app_user add column version bigint not null default 0;

create unique index ux_app_user_email_normalized on app_user (lower(btrim(email)));
update app_user set email = lower(btrim(email));
alter table app_user add constraint ck_user_email_normalized check (email = lower(btrim(email)));

create table password_reset_code (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references app_user(id) on delete cascade,
    code_hash varchar(255) not null,
    failed_attempts integer not null default 0 check (failed_attempts between 0 and 5),
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    consumed_at timestamptz,
    check (expires_at > created_at and expires_at <= created_at + interval '15 minutes')
);
create index idx_password_reset_user on password_reset_code(user_id, created_at desc);
create index idx_password_reset_expiration on password_reset_code(expires_at);
