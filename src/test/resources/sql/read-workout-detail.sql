select jsonb_build_object(
    'id', w.id, 'name', w.title, 'description', w.description,
    'datetime', w.scheduled_at, 'durationMinutes', w.duration_minutes,
    'createdBy', w.created_by, 'type', w.type, 'status', w.status,
    'stages', (
        select jsonb_agg(jsonb_build_object(
            'id', s.id, 'position', s.position, 'stageFormatId', s.format_id,
            'format', f.type, 'params', s.params,
            'rounds', (
                select jsonb_agg(jsonb_build_object(
                    'id', r.id, 'position', r.position, 'repeatCount', r.repeat_count,
                    'exercises', (
                        select jsonb_agg(jsonb_build_object(
                            'id', e.id, 'position', e.position,
                            'parameters', jsonb_build_object(
                                'exerciseId', e.exercise_id, 'repetitions', e.repetitions,
                                'weightKg', e.weight_kg, 'durationMinutes', e.duration_minutes,
                                'distanceMeters', e.distance_meters, 'heightCm', e.height_cm,
                                'notes', e.notes
                            )
                        ) order by e.position)
                        from round_exercise e where e.round_id = r.id
                    )
                ) order by r.position)
                from stage_round r where r.stage_id = s.id
            )
        ) order by s.position)
        from stage s join workout_format f on f.type = s.workout_format_type
        where s.workout_id = w.id
    )
)::text
from workout w where w.id = ?
