create table if not exists workout_format (
    type varchar(32) primary key,
    name varchar(255) not null,
    description text not null,
    duration_required boolean not null,
    interval_required boolean not null
);
insert into workout_format(type, name, description, duration_required, interval_required)
select code, name, coalesce(description, ''), true, code in ('EMOM', 'TABATA')
from complex_format
on conflict (type) do nothing;

alter table stage
    add column workout_format_type varchar(32),
    add column params jsonb;

update stage s
set workout_format_type = f.code,
    params = jsonb_strip_nulls(jsonb_build_object(
        'duration', s.time_limit_seconds,
        'interval', s.interval_seconds
    ))
from complex_format f
where f.id = s.complex_format_id;

alter table stage
    alter column workout_format_type set not null,
    alter column params set not null,
    alter column complex_format_id drop not null,
    add constraint fk_stage_workout_format foreign key (workout_format_type) references workout_format(type),
    add constraint ck_stage_params_object check (jsonb_typeof(params) = 'object');

create index idx_stage_workout_format on stage(workout_format_type);
alter table complex_format rename to legacy_complex_format;
