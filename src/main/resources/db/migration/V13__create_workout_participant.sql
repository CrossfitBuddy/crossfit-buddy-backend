create table workout_participant (
  id uuid primary key default gen_random_uuid(),
  workout_id uuid not null references workout(id) on delete cascade,
  trainer_id uuid references user_account(id) on delete set null,
  athlete_id uuid references user_account(id) on delete set null,
  unique (workout_id, trainer_id, athlete_id)
);

create index idx_workout_participant_athlete on workout_participant(athlete_id);

create index idx_workout_participant_trainer on workout_participant(trainer_id);
