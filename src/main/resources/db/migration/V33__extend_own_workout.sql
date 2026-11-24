alter table workout
    add column scheduled_at timestamptz,
    add column duration_seconds integer,
    add column coach text,
    add constraint ck_workout_duration_positive check (duration_seconds > 0);

create index idx_workout_owner_schedule on workout (created_by, scheduled_at, id);

alter table complex add column position integer;
with ordered as (
    select id, row_number() over (partition by workout_id order by created_at, id)::integer as position
    from complex
)
update complex set position = ordered.position from ordered where complex.id = ordered.id;
alter table complex
    alter column position set not null,
    add constraint ck_complex_position_positive check (position > 0),
    add constraint uq_complex_workout_position unique (workout_id, position);
