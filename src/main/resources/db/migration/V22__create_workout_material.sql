create type material_type as enum ('VIDEO', 'LINK', 'FILE');
create table workout_material (
    id uuid primary key default gen_random_uuid(),
    workout_id uuid not null references workout(id) on delete cascade,
    material_type material_type not null,
    title text not null,
    url text not null,
);
create index idx_workout_material_workout on workout_material(workout_id);
