alter table workout add column duration_minutes numeric;
update workout set duration_minutes = duration_seconds::numeric / 60
where duration_seconds is not null;
alter table workout add constraint ck_workout_duration_minutes_positive check (duration_minutes > 0);

update stage
set params = case when params ? 'duration'
    then jsonb_set(params, '{duration}', to_jsonb((params->>'duration')::numeric / 60))
    else params end;
update stage
set params = case when params ? 'interval'
    then jsonb_set(params, '{interval}', to_jsonb((params->>'interval')::numeric / 60))
    else params end;

alter table round_exercise add column duration_minutes numeric;
update round_exercise set duration_minutes = duration_seconds::numeric / 60
where duration_seconds is not null;
alter table round_exercise add constraint ck_round_exercise_duration_minutes_positive check (duration_minutes > 0) not valid;
