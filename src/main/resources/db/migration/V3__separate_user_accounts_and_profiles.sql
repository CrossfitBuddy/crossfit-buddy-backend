alter table app_user rename to user_account;

create table user_profile (
    user_id uuid primary key references user_account(id) on delete cascade,
    first_name varchar(100),
    last_name varchar(100),
    birth_date date,
    gender varchar(20) check (gender in ('MALE', 'FEMALE', 'OTHER')),
    city varchar(120),
    about text check (char_length(about) <= 5000),
    avatar_key varchar(512),
    display_name varchar(201),
    version bigint not null default 0
);

insert into user_profile (user_id, first_name, last_name, display_name)
select id, first_name, last_name, display_name from user_account;

create table athlete_profile (
    user_id uuid primary key references user_account(id) on delete cascade,
    height_cm integer check (height_cm > 0),
    weight_kg numeric(6,2) check (weight_kg > 0),
    fitness_level varchar(20) check (fitness_level in ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    rx_preference varchar(20) check (rx_preference in ('RX', 'SCALED')),
    version bigint not null default 0
);

create table trainer_profile (
    user_id uuid primary key references user_account(id) on delete cascade,
    specializations text check (char_length(specializations) <= 5000),
    experience_years integer check (experience_years >= 0),
    methodology text check (char_length(methodology) <= 5000),
    version bigint not null default 0
);

insert into athlete_profile(user_id) select user_id from user_roles where role = 'ATHLETE';
insert into trainer_profile(user_id) select user_id from user_roles where role = 'TRAINER';

alter table user_account drop column first_name, drop column last_name, drop column display_name;
