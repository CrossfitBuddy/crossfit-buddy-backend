begin;

alter table stage_format rename to workout_stage;

alter table workout_stage rename constraint stage_format_pkey to workout_stage_pkey;
alter table workout_stage rename constraint stage_format_code_key to workout_stage_code_key;
alter table workout_stage rename constraint stage_format_position_check to workout_stage_position_check;

alter table stage rename to complex;
alter table complex rename column format_id to workout_stage_id;
alter table complex rename constraint stage_pkey to complex_pkey;
alter table complex rename constraint stage_complex_format_type_fkey to complex_complex_format_type_fkey;
alter table complex rename constraint stage_workout_id_fkey to complex_workout_id_fkey;
alter table complex rename constraint stage_workout_stage_id_fkey to complex_workout_stage_id_fkey;
alter table complex rename constraint stage_format_id_fkey to stage_workout_stage_id_fkey;


alter table stage_round rename to complex_round;
alter table complex_round rename column stage_id to complex_id;
alter table complex_round rename constraint stage_round_pkey to complex_round_pkey;
alter table complex_round rename constraint stage_round_stage_id_position_key to complex_round_complex_id_position_key;
alter table complex_round rename constraint stage_round_stage_id_fkey to complex_round_complex_id_fkey;
alter table complex_round rename constraint stage_round_position_check to complex_round_position_check;
alter table complex_round rename constraint stage_round_repeat_count_check to complex_round_repeat_count_check;


alter table stage_result rename to complex_result;
alter table complex_result rename column stage_id to complex_id;
alter table complex_result rename constraint stage_result_pkey to complex_result_pkey;
alter table complex_result rename constraint stage_result_athlete_id_fkey to complex_result_athlete_id_fkey;
alter table complex_result rename constraint stage_result_stage_id_fkey to complex_result_complex_id_fkey;

alter index idx_stage_result_athlete rename to idx_complex_result_athlete;

insert into workout_stage (id, code, name, description, position)
values
    (
        1,
        'WARM_UP',
        'Warm-up',
        'Разминка для подготовки организма к основной физической нагрузке',
        1
    ),
    (
        2,
        'SKILL_STRENGTH',
        ' Skill/Strength',
        'Развитие силовых показателей и отработка техники выполнения упражнений',
        2
    ),
    (
        3,
        'WOD',
        'WOD',
        'Основной тренировочный комплекс (Workout of the Day)',
        3
    ),
    (
        3,
        'COOL_DOWN',
        'Cool-down',
        'Заминка для восстановления организма после физической нагрузки',
        4
    )
on conflict (id) do nothing;

commit;
