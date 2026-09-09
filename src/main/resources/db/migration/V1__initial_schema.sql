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

create table workout_format (
  id uuid primary key default gen_random_uuid(),
  code varchar(40) not null unique,
  name varchar(120) not null,
  description text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table workout (
  id uuid primary key default gen_random_uuid(),
  title varchar(180) not null,
  description text,
  type workout_type not null,
  status workout_status not null default 'DRAFT',
  format_id uuid references workout_format(id),
  created_by uuid not null references app_user(id),
  assigned_to uuid references app_user(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table workout_round (
  id uuid primary key default gen_random_uuid(),
  workout_id uuid not null references workout(id) on delete cascade,
  position integer not null check (position > 0),
  repeat_count integer not null default 1 check (repeat_count > 0),
  unique (workout_id, position)
);

create table round_exercise (
  id uuid primary key default gen_random_uuid(),
  round_id uuid not null references workout_round(id) on delete cascade,
  exercise_id uuid not null references exercise(id),
  position integer not null check (position > 0),
  repetitions integer,
  weight_kg numeric(7,2),
  duration_seconds integer,
  distance_meters integer,
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

create table workout_result (
  id uuid primary key default gen_random_uuid(),
  workout_id uuid not null references workout(id),
  athlete_id uuid not null references app_user(id),
  started_at timestamptz not null,
  completed_at timestamptz,
  elapsed_seconds integer,
  rounds_completed integer,
  repetitions_completed integer,
  notes text,
  created_at timestamptz not null default now()
);

create index idx_workout_created_by on workout(created_by);
create index idx_workout_assigned_to on workout(assigned_to);
create index idx_result_athlete on workout_result(athlete_id, started_at desc);
create index idx_subscription_trainer_status on trainer_subscription(trainer_id, status);
