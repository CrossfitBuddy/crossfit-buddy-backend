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
