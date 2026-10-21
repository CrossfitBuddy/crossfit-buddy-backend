create table trainer_profile (
    user_id uuid primary key references user_account(id) on delete cascade,
    specializations text check (char_length(specializations) <= 5000),
    experience_years integer constraint ck_trainer_experience_range check (experience_years between 0 and 80),
    methodology text check (char_length(methodology) <= 5000),
    version bigint not null default 0
);
