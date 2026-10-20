create table athlete_profile (
    user_id uuid primary key references user_account(id) on delete cascade,
    height_cm integer constraint ck_athlete_height_range check (height_cm between 50 and 250),
    weight_kg numeric(6,2) constraint ck_athlete_weight_range check (weight_kg between 20 and 500),
    fitness_level varchar(20) check (fitness_level in ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    rx_preference varchar(20) check (rx_preference in ('RX', 'SCALED')),
    version bigint not null default 0
);
