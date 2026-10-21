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
