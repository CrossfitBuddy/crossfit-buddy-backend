create table trainer_subscription (
  id uuid primary key default gen_random_uuid(),
  athlete_id uuid not null references user_account(id),
  trainer_id uuid not null references user_account(id),
  status subscription_status not null default 'PENDING',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (athlete_id <> trainer_id),
  unique (athlete_id, trainer_id)
);

create index idx_subscription_trainer_status on trainer_subscription(trainer_id, status);
