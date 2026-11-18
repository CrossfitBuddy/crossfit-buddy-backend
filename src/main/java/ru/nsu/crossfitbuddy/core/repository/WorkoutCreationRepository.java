package ru.nsu.crossfitbuddy.core.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutFormatType;
import ru.nsu.crossfitbuddy.core.workout.model.WorkoutToCreate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WorkoutCreationRepository {
    private final JdbcTemplate database;
    private final NamedParameterJdbcTemplate namedDatabase;
    private final ObjectMapper json;

    public record FormatDefinition(boolean durationRequired, boolean intervalRequired) {}

    public Set<Integer> lockStageFormats(Set<Integer> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(namedDatabase.queryForList(
            """
            select id
            from stage_format
            where id in (:ids)
            order by id for key share
            """,
            Map.of("ids", ids), Integer.class));
    }

    public Map<WorkoutFormatType, FormatDefinition> lockWorkoutFormats(
        Set<WorkoutFormatType> codes) {
        if (codes.isEmpty()) {
            return Map.of();
        }
        Map<WorkoutFormatType, FormatDefinition> result = new HashMap<>();
        namedDatabase.query(
            """
            select type, duration_required, interval_required
            from workout_format
            where type in (:codes)
            order by type for share
            """,
            Map.of("codes", codes.stream().map(Enum::name).toList()),
            row -> {
                var type = WorkoutFormatType.valueOf(row.getString("type"));
                var definition = new FormatDefinition(
                    row.getBoolean("duration_required"), row.getBoolean("interval_required"));
                result.put(type, definition);
            });
        return result;
    }

    public Set<UUID> lockAvailableExercises(Set<UUID> ids, UUID owner) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(namedDatabase.queryForList(
            """
            select id
            from exercise
            where id in (:ids) and (is_common or created_by = :owner)
            order by id for share
            """,
            Map.of("ids", ids, "owner", owner), UUID.class));
    }

    public void save(WorkoutToCreate workout) {
        insertWorkout(workout);
        insertStages(workout);
        insertRounds(workout);
        insertExercises(workout);
    }

    private void insertWorkout(WorkoutToCreate workout) {
        database.update("""
            insert into workout(id, title, description, scheduled_at, duration_minutes, created_by, type, status)
            values (?, ?, ?, ?, ?, ?, ?::workout_type, ?::workout_status)
            """, workout.id(), workout.name(), workout.description(), workout.datetime(),
            workout.durationMinutes(), workout.createdBy(), workout.type().name(), workout.status().name());
    }

    private void insertStages(WorkoutToCreate workout) {
        var rows = new ArrayList<Object[]>();
        for (var stage : workout.stages()) {
            rows.add(new Object[] {stage.id(), workout.id(), stage.stageFormatId(), stage.format().name(),
                stage.position(), writeParams(stage.params())});
        }
        database.batchUpdate("""
            insert into stage(id, workout_id, format_id, workout_format_type, position, params)
            values (?, ?, ?, ?, ?, ?::jsonb)
            """, rows);
    }

    private void insertRounds(WorkoutToCreate workout) {
        var rows = new ArrayList<Object[]>();
        for (var stage : workout.stages()) {
            for (var round : stage.rounds()) {
                rows.add(new Object[] {round.id(), stage.id(), round.position(), round.repeatCount()});
            }
        }
        database.batchUpdate("""
            insert into stage_round(id, stage_id, position, repeat_count)
            values (?, ?, ?, ?)
            """, rows);
    }

    private void insertExercises(WorkoutToCreate workout) {
        var rows = new ArrayList<Object[]>();
        for (var stage : workout.stages()) {
            for (var round : stage.rounds()) {
                for (var exercise : round.exercises()) {
                    rows.add(new Object[] {exercise.id(), round.id(), exercise.exerciseId(), exercise.position(),
                        exercise.repetitions(), exercise.weightKg(), exercise.durationMinutes(),
                        exercise.distanceMeters(), exercise.heightCm(), exercise.notes()});
                }
            }
        }
        database.batchUpdate("""
            insert into round_exercise(id, round_id, exercise_id, position, repetitions, weight_kg,
                duration_minutes, distance_meters, height_cm, notes)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, rows);
    }

    private String writeParams(Map<String, BigDecimal> params) {
        try {
            return json.writeValueAsString(params);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize validated workout format parameters.", exception);
        }
    }
}
