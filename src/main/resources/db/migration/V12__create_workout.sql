create table workout (
  id uuid primary key default gen_random_uuid(),
  title text,
  description text,
  type workout_type not null,
  status workout_status not null default 'DRAFT',
  created_by uuid not null references user_account(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_workout_created_by on workout(created_by);
