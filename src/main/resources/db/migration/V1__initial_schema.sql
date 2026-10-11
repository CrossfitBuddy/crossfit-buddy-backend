create extension if not exists pgcrypto;

create type user_role as enum ('ATHLETE', 'TRAINER', 'ADMIN');
create type workout_type as enum ('OWN', 'ASSIGNED', 'SHARED');
create type workout_status as enum ('DRAFT', 'READY', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');
create type subscription_status as enum ('PENDING', 'ACTIVE', 'REJECTED', 'CANCELLED');

create table app_user (
  id uuid primary key default gen_random_uuid(),
  email varchar(320) not null unique,
  password_hash varchar(255) not null,
  display_name varchar(120) not null,
  active_role user_role,
  enabled boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table user_roles (
  user_id uuid not null references app_user(id) on delete cascade,
  role user_role not null,
  primary key (user_id, role)
);

create table exercise (
  id uuid primary key default gen_random_uuid(),
  name varchar(160) not null,
  description text,
  created_by uuid references app_user(id),
  is_common boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table stage_format (
  id integer primary key,
  code varchar(40) not null unique,
  name varchar(120) not null,
  description text,
  position integer not null check (position >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table workout (
  id uuid primary key default gen_random_uuid(),
  title text,
  description text,
  type workout_type not null,
  status workout_status not null default 'DRAFT',
  created_by uuid not null references app_user(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table workout_participant (
  id uuid primary key default gen_random_uuid(),
  workout_id uuid not null references workout(id) on delete cascade,
  trainer_id uuid references app_user(id) on delete set null,
  athlete_id uuid references app_user(id) on delete set null,
  unique (workout_id, trainer_id, athlete_id)
);

create table complex_format (
  id integer primary key,
  code varchar(40) not null unique,
  name varchar(120) not null,
  description text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table stage (
  id uuid primary key default gen_random_uuid(),
  workout_id uuid not null references workout(id) on delete cascade,
  format_id integer not null references stage_format(id),
  complex_format_id integer not null references complex_format(id),
  time_limit_seconds integer,
  target_rounds integer,
  interval_seconds integer,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table stage_round (
  id uuid primary key default gen_random_uuid(),
  stage_id uuid not null references stage(id) on delete cascade,
  position integer not null check (position > 0),
  repeat_count integer not null default 1 check (repeat_count > 0),
  unique (stage_id, position)
);

create table round_exercise (
  id uuid primary key default gen_random_uuid(),
  round_id uuid not null references stage_round(id) on delete cascade,
  exercise_id uuid not null references exercise(id),
  position integer not null check (position > 0),
  repetitions integer,
  weight_kg numeric(7,2),
  duration_seconds integer,
  distance_meters integer,
  height_cm integer,
  notes text,
  unique (round_id, position)
);

create table trainer_subscription (
  id uuid primary key default gen_random_uuid(),
  athlete_id uuid not null references app_user(id),
  trainer_id uuid not null references app_user(id),
  status subscription_status not null default 'PENDING',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (athlete_id <> trainer_id),
  unique (athlete_id, trainer_id)
);

create table stage_result (
  id uuid primary key default gen_random_uuid(),
  stage_id uuid not null references stage(id),
  athlete_id uuid not null references app_user(id),
  is_rx boolean not null default false,
  is_completed boolean not null default true,
  elapsed_seconds integer,
  rounds_completed integer,
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_workout_created_by on workout(created_by);
create index idx_workout_participant_athlete on workout_participant(athlete_id);
create index idx_workout_participant_trainer on workout_participant(trainer_id);
create index idx_stage_result_athlete on stage_result(athlete_id);
create index idx_subscription_trainer_status on trainer_subscription(trainer_id, status);
