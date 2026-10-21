create table stage_result (
  id uuid primary key default gen_random_uuid(),
  stage_id uuid not null references stage(id),
  athlete_id uuid not null references user_account(id),
  is_rx boolean not null default false,
  is_completed boolean not null default true,
  elapsed_seconds integer,
  rounds_completed integer,
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_stage_result_athlete on stage_result(athlete_id);
