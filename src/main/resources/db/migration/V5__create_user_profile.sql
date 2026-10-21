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
