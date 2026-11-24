select jsonb_build_object(
    'id', w.id, 'name', w.title, 'description', w.description, 'coach', w.coach,
    'datetime', w.scheduled_at, 'durationSeconds', w.duration_seconds,
    'createdBy', w.created_by, 'type', w.type, 'status', w.status,
    'stages', (
        select jsonb_agg(jsonb_build_object(
            'id', s.id, 'position', s.position, 'workoutStageId', s.workout_stage_id,
            'format', f.type, 'timeLimitSeconds', s.time_limit_seconds,
            'intervalSeconds', s.interval_seconds, 'targetRounds', s.target_rounds,
            'rounds', (
                select jsonb_agg(jsonb_build_object(
                    'id', r.id, 'position', r.position, 'repeatCount', r.repeat_count,
                    'exercises', (
                        select jsonb_agg(jsonb_build_object(
                            'id', e.id, 'position', e.position,
                            'parameters', jsonb_build_object(
                                'exerciseId', e.exercise_id, 'repetitions', e.repetitions,
                                'weightKg', e.weight_kg, 'durationSeconds', e.duration_seconds,
                                'distanceMeters', e.distance_meters, 'heightCm', e.height_cm,
                                'notes', e.notes
                            )
                        ) order by e.position)
                        from round_exercise e where e.round_id = r.id
                    )
                ) order by r.position)
                from complex_round r where r.complex_id = s.id
            )
        ) order by s.position)
        from complex s join complex_format f on f.type = s.complex_format_type
        where s.workout_id = w.id
    )
)::text
from workout w where w.id = ?
