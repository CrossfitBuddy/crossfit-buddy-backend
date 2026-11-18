alter table workout
    add column scheduled_at timestamptz,
    add column duration_seconds integer,
    add constraint ck_workout_duration_positive check (duration_seconds > 0);

create index idx_workout_owner_schedule on workout (created_by, scheduled_at, id);
alter table stage add column position integer;

with ordered as (
    select id, row_number() over (
        partition by workout_id order by created_at, id
    )::integer as position
    from stage
)
update stage
set position = ordered.position
from ordered
where stage.id = ordered.id;

alter table stage
    alter column position set not null,
    add constraint ck_stage_position_positive check (position > 0),
    add constraint uq_stage_workout_position unique (workout_id, position);
