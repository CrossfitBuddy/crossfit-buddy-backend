create table stage_round (
  id uuid primary key default gen_random_uuid(),
  stage_id uuid not null references stage(id) on delete cascade,
  position integer not null check (position > 0),
  repeat_count integer not null default 1 check (repeat_count > 0),
  unique (stage_id, position)
);
