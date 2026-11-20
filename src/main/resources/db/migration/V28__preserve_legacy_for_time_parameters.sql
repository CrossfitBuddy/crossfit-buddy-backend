alter table stage add column legacy_params jsonb;

update stage
set legacy_params = params, params = '{}'::jsonb
where workout_format_type = 'FOR_TIME' and params <> '{}'::jsonb;
