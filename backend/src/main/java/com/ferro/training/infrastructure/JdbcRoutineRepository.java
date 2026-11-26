package com.ferro.training.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ferro.training.application.port.RoutineRepository;
import com.ferro.training.domain.Routine;
import com.ferro.training.domain.RoutineExercise;
import com.ferro.training.domain.RoutineNameTakenException;
import com.ferro.training.domain.RoutineSet;
import com.ferro.training.domain.RoutineSummary;
import com.ferro.training.domain.SetType;

@Repository
public class JdbcRoutineRepository implements RoutineRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcRoutineRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<RoutineSummary> findAll(UUID userId, int page, int size) {
        return jdbc.query(
                """
                SELECT r.id, r.name, count(re.id) AS exercise_count,
                    (SELECT max(ws.started_at) FROM workout_sessions ws
                     WHERE ws.routine_id = r.id) AS last_used_at
                FROM routines r
                LEFT JOIN routine_exercises re ON re.routine_id = r.id
                WHERE r.user_id = :userId
                GROUP BY r.id
                ORDER BY r.updated_at DESC, r.name
                LIMIT :size OFFSET :offset
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("size", size)
                        .addValue("offset", page * size),
                (rs, rowNum) -> new RoutineSummary(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getInt("exercise_count"),
                        instant(rs.getTimestamp("last_used_at"))));
    }

    @Override
    public long count(UUID userId) {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM routines WHERE user_id = :userId",
                new MapSqlParameterSource("userId", userId),
                Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public boolean nameExists(UUID userId, String name) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM routines WHERE user_id = :userId AND lower(name) = lower(:name)",
                new MapSqlParameterSource().addValue("userId", userId).addValue("name", name),
                Integer.class);
        return count != null && count > 0;
    }

    @Override
    public Optional<Routine> find(UUID userId, UUID routineId) {
        List<RoutineRow> rows = jdbc.query(
                "SELECT id, name, notes FROM routines WHERE id = :id AND user_id = :userId",
                new MapSqlParameterSource().addValue("id", routineId).addValue("userId", userId),
                (rs, rowNum) ->
                        new RoutineRow(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("notes")));
        return rows.stream()
                .findFirst()
                .map(row -> new Routine(row.id(), row.name(), row.notes(), exercises(row.id())));
    }

    @Override
    public void create(UUID userId, Routine routine) {
        try {
            jdbc.update(
                    """
                    INSERT INTO routines (id, user_id, name, notes)
                    VALUES (:id, :userId, :name, :notes)
                    """,
                    new MapSqlParameterSource()
                            .addValue("id", routine.id())
                            .addValue("userId", userId)
                            .addValue("name", routine.name())
                            .addValue("notes", routine.notes()));
            insertExercises(routine);
        } catch (DuplicateKeyException exception) {
            throw new RoutineNameTakenException();
        }
    }

    @Override
    public void replace(UUID userId, Routine routine) {
        try {
            jdbc.update(
                    """
                    UPDATE routines SET name = :name, notes = :notes, updated_at = now()
                    WHERE id = :id AND user_id = :userId
                    """,
                    new MapSqlParameterSource()
                            .addValue("id", routine.id())
                            .addValue("userId", userId)
                            .addValue("name", routine.name())
                            .addValue("notes", routine.notes()));
        } catch (DuplicateKeyException exception) {
            throw new RoutineNameTakenException();
        }
        jdbc.update(
                "DELETE FROM routine_exercises WHERE routine_id = :id", new MapSqlParameterSource("id", routine.id()));
        insertExercises(routine);
    }

    @Override
    public void delete(UUID userId, UUID routineId) {
        jdbc.update(
                "DELETE FROM routines WHERE id = :id AND user_id = :userId",
                new MapSqlParameterSource().addValue("id", routineId).addValue("userId", userId));
    }

    private void insertExercises(Routine routine) {
        for (RoutineExercise exercise : routine.exercises()) {
            jdbc.update(
                    """
                    INSERT INTO routine_exercises
                        (id, routine_id, exercise_id, position, rest_seconds, superset_group, notes)
                    VALUES (:id, :routineId, :exerciseId, :position, :restSeconds, :supersetGroup, :notes)
                    """,
                    new MapSqlParameterSource()
                            .addValue("id", exercise.id())
                            .addValue("routineId", routine.id())
                            .addValue("exerciseId", exercise.exerciseId())
                            .addValue("position", exercise.position())
                            .addValue("restSeconds", exercise.restSeconds())
                            .addValue("supersetGroup", exercise.supersetGroup())
                            .addValue("notes", exercise.notes()));
            for (RoutineSet set : exercise.sets()) {
                jdbc.update(
                        """
                        INSERT INTO routine_sets
                            (id, routine_exercise_id, position, set_type,
                             target_reps, target_weight, target_rir)
                        VALUES (:id, :entryId, :position, CAST(:type AS set_type),
                                :reps, :weight, :rir)
                        """,
                        new MapSqlParameterSource()
                                .addValue("id", set.id())
                                .addValue("entryId", exercise.id())
                                .addValue("position", set.position())
                                .addValue("type", set.setType().name())
                                .addValue("reps", set.targetReps())
                                .addValue("weight", set.targetWeight())
                                .addValue("rir", set.targetRir()));
            }
        }
    }

    private List<RoutineExercise> exercises(UUID routineId) {
        return jdbc.query(
                """
                SELECT re.id, re.exercise_id, e.name_es, re.position, re.rest_seconds,
                       re.superset_group, re.notes
                FROM routine_exercises re JOIN exercises e ON e.id = re.exercise_id
                WHERE re.routine_id = :id ORDER BY re.position
                """,
                new MapSqlParameterSource("id", routineId),
                (rs, rowNum) -> new RoutineExercise(
                        rs.getObject("id", UUID.class),
                        rs.getObject("exercise_id", UUID.class),
                        rs.getString("name_es"),
                        rs.getInt("position"),
                        (Integer) rs.getObject("rest_seconds"),
                        (Integer) rs.getObject("superset_group"),
                        rs.getString("notes"),
                        sets(rs.getObject("id", UUID.class))));
    }

    private List<RoutineSet> sets(UUID exerciseId) {
        return jdbc.query(
                """
                SELECT id, position, set_type, target_reps, target_weight, target_rir
                FROM routine_sets WHERE routine_exercise_id = :id ORDER BY position
                """,
                new MapSqlParameterSource("id", exerciseId),
                (rs, rowNum) -> new RoutineSet(
                        rs.getObject("id", UUID.class),
                        rs.getInt("position"),
                        SetType.valueOf(rs.getString("set_type")),
                        rs.getInt("target_reps"),
                        rs.getBigDecimal("target_weight"),
                        (Integer) rs.getObject("target_rir")));
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record RoutineRow(UUID id, String name, String notes) {}
}
