package com.ferro.training.infrastructure;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ferro.shared.domain.UuidV7;
import com.ferro.training.application.port.TrainingRepository;
import com.ferro.training.domain.NewPersonalRecord;
import com.ferro.training.domain.PrType;
import com.ferro.training.domain.PreviousSession;
import com.ferro.training.domain.PreviousSet;
import com.ferro.training.domain.SessionAlreadyActiveException;
import com.ferro.training.domain.SessionExercise;
import com.ferro.training.domain.SessionNotFoundException;
import com.ferro.training.domain.SessionSet;
import com.ferro.training.domain.SessionStatus;
import com.ferro.training.domain.SetMath;
import com.ferro.training.domain.SetType;
import com.ferro.training.domain.StaleSession;
import com.ferro.training.domain.TargetSet;
import com.ferro.training.domain.WorkoutSession;

@Repository
public class JdbcTrainingRepository implements TrainingRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcTrainingRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<WorkoutSession> findActive(UUID userId) {
        List<UUID> ids = jdbc.query(
                "SELECT id FROM workout_sessions WHERE user_id = :userId AND status = 'ACTIVE'",
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> rs.getObject("id", UUID.class));
        return ids.stream().findFirst().flatMap(id -> find(userId, id));
    }

    @Override
    public Optional<WorkoutSession> find(UUID userId, UUID sessionId) {
        List<Row> rows = jdbc.query(
                """
                SELECT id, name, notes, status, started_at, finished_at, duration_seconds, total_volume
                FROM workout_sessions WHERE id = :sessionId AND user_id = :userId
                """,
                new MapSqlParameterSource("sessionId", sessionId).addValue("userId", userId),
                (rs, rowNum) -> new Row(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("notes"),
                        SessionStatus.valueOf(rs.getString("status")),
                        instant(rs.getTimestamp("started_at")),
                        instant(rs.getTimestamp("finished_at")),
                        (Integer) rs.getObject("duration_seconds"),
                        rs.getBigDecimal("total_volume")));
        return rows.stream().findFirst().map(row -> hydrate(row));
    }

    @Override
    public WorkoutSession create(UUID userId, UUID sessionId, String name, UUID routineId, Instant now) {
        List<UUID> active = jdbc.query(
                "SELECT id FROM workout_sessions WHERE user_id = :userId AND status = 'ACTIVE'",
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> rs.getObject("id", UUID.class));
        if (!active.isEmpty()) {
            throw new SessionAlreadyActiveException(active.getFirst());
        }
        try {
            jdbc.update(
                    """
                    INSERT INTO workout_sessions
                        (id, user_id, routine_id, name, status, started_at, total_volume)
                    VALUES (:id, :userId, :routineId, :name, 'ACTIVE', :now, 0)
                    """,
                    new MapSqlParameterSource()
                            .addValue("id", sessionId)
                            .addValue("userId", userId)
                            .addValue("routineId", routineId)
                            .addValue("name", name)
                            .addValue("now", Timestamp.from(now)));
        } catch (DuplicateKeyException exception) {
            // Carrera entre dos peticiones: no se puede consultar dentro de la transacción abortada.
            throw new SessionAlreadyActiveException(null);
        }
        return find(userId, sessionId).orElseThrow(SessionNotFoundException::new);
    }

    @Override
    public int exerciseCount(UUID sessionId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM session_exercises WHERE session_id = :sessionId",
                new MapSqlParameterSource("sessionId", sessionId),
                Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public int setCount(UUID sessionExerciseId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM session_sets WHERE session_exercise_id = :id",
                new MapSqlParameterSource("id", sessionExerciseId),
                Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public void addExercise(
            UUID sessionId,
            UUID entryId,
            UUID exerciseId,
            int position,
            Integer restSeconds,
            Integer supersetGroup,
            String notes) {
        jdbc.update(
                """
                INSERT INTO session_exercises
                    (id, session_id, exercise_id, position, rest_seconds, superset_group, notes)
                VALUES (:id, :sessionId, :exerciseId, :position, :restSeconds, :supersetGroup, :notes)
                """,
                new MapSqlParameterSource()
                        .addValue("id", entryId)
                        .addValue("sessionId", sessionId)
                        .addValue("exerciseId", exerciseId)
                        .addValue("position", position)
                        .addValue("restSeconds", restSeconds)
                        .addValue("supersetGroup", supersetGroup)
                        .addValue("notes", notes));
    }

    @Override
    public void addTargetSet(
            UUID entryId,
            UUID targetSetId,
            SetType setType,
            int targetReps,
            BigDecimal targetWeight,
            Integer targetRir,
            int position) {
        jdbc.update(
                """
                INSERT INTO session_target_sets
                    (id, session_exercise_id, position, set_type, target_reps, target_weight, target_rir)
                VALUES (:id, :entryId, :position, CAST(:type AS set_type), :reps, :weight, :rir)
                """,
                new MapSqlParameterSource()
                        .addValue("id", targetSetId)
                        .addValue("entryId", entryId)
                        .addValue("position", position)
                        .addValue("type", setType.name())
                        .addValue("reps", targetReps)
                        .addValue("weight", targetWeight)
                        .addValue("rir", targetRir));
    }

    @Override
    public void deleteTargetSet(UUID targetSetId) {
        jdbc.update("DELETE FROM session_target_sets WHERE id = :id", new MapSqlParameterSource("id", targetSetId));
    }

    @Override
    public SessionSet addSet(
            UUID entryId,
            UUID setId,
            SetType setType,
            int reps,
            BigDecimal weight,
            Integer rir,
            BigDecimal volume,
            BigDecimal e1rm,
            Instant loggedAt,
            String notes) {
        Integer position = jdbc.queryForObject(
                "SELECT coalesce(max(position), -1) + 1 FROM session_sets WHERE session_exercise_id = :id",
                new MapSqlParameterSource("id", entryId),
                Integer.class);
        int next = position == null ? 0 : position;
        jdbc.update(
                """
                INSERT INTO session_sets
                    (id, session_exercise_id, position, set_type, reps, weight, rir, logged_at, notes)
                VALUES (:id, :entryId, :position, CAST(:type AS set_type), :reps, :weight, :rir, :loggedAt, :notes)
                """,
                new MapSqlParameterSource()
                        .addValue("id", setId)
                        .addValue("entryId", entryId)
                        .addValue("position", next)
                        .addValue("type", setType.name())
                        .addValue("reps", reps)
                        .addValue("weight", weight)
                        .addValue("rir", rir)
                        .addValue("loggedAt", Timestamp.from(loggedAt))
                        .addValue("notes", notes));
        return new SessionSet(setId, next, setType, reps, weight, rir, volume, e1rm, loggedAt, notes);
    }

    @Override
    public BigDecimal totalVolume(UUID sessionId) {
        BigDecimal volume =
                jdbc.queryForObject("""
                SELECT coalesce(sum(CASE WHEN ss.set_type <> 'WARMUP' AND ss.weight > 0
                    THEN ss.weight * ss.reps ELSE 0 END), 0)
                FROM session_sets ss
                JOIN session_exercises se ON se.id = ss.session_exercise_id
                WHERE se.session_id = :sessionId
                """, new MapSqlParameterSource("sessionId", sessionId), BigDecimal.class);
        return volume == null ? BigDecimal.ZERO : volume;
    }

    @Override
    public void finish(UUID sessionId, Instant finishedAt, int durationSeconds, BigDecimal totalVolume) {
        jdbc.update(
                """
                UPDATE workout_sessions
                SET status = 'FINISHED', finished_at = :finishedAt, duration_seconds = :duration,
                    total_volume = :volume, updated_at = now()
                WHERE id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("id", sessionId)
                        .addValue("finishedAt", Timestamp.from(finishedAt))
                        .addValue("duration", durationSeconds)
                        .addValue("volume", totalVolume));
    }

    @Override
    public void discard(UUID sessionId, Instant now) {
        jdbc.update("""
                UPDATE workout_sessions
                SET status = 'DISCARDED', finished_at = :now, updated_at = now()
                WHERE id = :id
                """, new MapSqlParameterSource("id", sessionId).addValue("now", Timestamp.from(now)));
    }

    @Override
    public List<NewPersonalRecord> recalculatePersonalRecords(UUID userId, UUID sessionId, Instant now) {
        List<NewPersonalRecord> created = new ArrayList<>();
        List<ExerciseRow> exercises = jdbc.query(
                """
                SELECT DISTINCT se.exercise_id, e.name_es
                FROM session_exercises se JOIN exercises e ON e.id = se.exercise_id
                WHERE se.session_id = :sessionId
                """,
                new MapSqlParameterSource("sessionId", sessionId),
                (rs, rowNum) -> new ExerciseRow(rs.getObject("exercise_id", UUID.class), rs.getString("name_es")));

        for (ExerciseRow exercise : exercises) {
            List<SetRow> sets = jdbc.query(
                    """
                    SELECT ss.id, ss.reps, ss.weight, ss.logged_at, ss.set_type
                    FROM session_sets ss
                    JOIN session_exercises se ON se.id = ss.session_exercise_id
                    WHERE se.session_id = :sessionId AND se.exercise_id = :exerciseId
                    """,
                    new MapSqlParameterSource().addValue("sessionId", sessionId).addValue("exerciseId", exercise.id()),
                    (rs, rowNum) -> new SetRow(
                            rs.getObject("id", UUID.class),
                            rs.getInt("reps"),
                            rs.getBigDecimal("weight"),
                            instant(rs.getTimestamp("logged_at")),
                            SetType.valueOf(rs.getString("set_type"))));

            BigDecimal maxWeight = null;
            UUID maxWeightSet = null;
            BigDecimal maxE1rm = null;
            UUID maxE1rmSet = null;
            for (SetRow set : sets) {
                if (set.type() == SetType.WARMUP
                        || set.weight() == null
                        || set.weight().signum() <= 0) {
                    continue;
                }
                if (maxWeight == null || set.weight().compareTo(maxWeight) > 0) {
                    maxWeight = set.weight();
                    maxWeightSet = set.id();
                }
                BigDecimal e1rm = SetMath.e1rm(set.type(), set.weight(), set.reps());
                if (e1rm != null && (maxE1rm == null || e1rm.compareTo(maxE1rm) > 0)) {
                    maxE1rm = e1rm;
                    maxE1rmSet = set.id();
                }
            }

            created.addAll(
                    insertIfBetter(userId, sessionId, exercise, PrType.MAX_WEIGHT, maxWeight, maxWeightSet, now));
            created.addAll(insertIfBetter(userId, sessionId, exercise, PrType.MAX_E1RM, maxE1rm, maxE1rmSet, now));
        }
        return created;
    }

    private List<NewPersonalRecord> insertIfBetter(
            UUID userId, UUID sessionId, ExerciseRow exercise, PrType type, BigDecimal value, UUID setId, Instant now) {
        if (value == null || setId == null) {
            return List.of();
        }
        BigDecimal previous = jdbc.queryForObject(
                """
                SELECT max(value) FROM personal_records
                WHERE user_id = :userId AND exercise_id = :exerciseId AND pr_type = CAST(:type AS pr_type)
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("exerciseId", exercise.id())
                        .addValue("type", type.name()),
                BigDecimal.class);
        if (previous != null && value.compareTo(previous) <= 0) {
            return List.of();
        }
        jdbc.update(
                """
                INSERT INTO personal_records
                    (id, user_id, exercise_id, pr_type, value, achieved_at, session_set_id)
                VALUES (:id, :userId, :exerciseId, CAST(:type AS pr_type), :value, :now, :setId)
                """,
                new MapSqlParameterSource()
                        .addValue("id", UuidV7.generate())
                        .addValue("userId", userId)
                        .addValue("exerciseId", exercise.id())
                        .addValue("type", type.name())
                        .addValue("value", value)
                        .addValue("now", Timestamp.from(now))
                        .addValue("setId", setId));
        return List.of(new NewPersonalRecord(exercise.id(), exercise.name(), type, value, previous, now));
    }

    @Override
    public Optional<ExerciseInfo> exercise(UUID exerciseId) {
        List<ExerciseInfo> info = jdbc.query(
                "SELECT id, name_es, bodyweight, is_custom, owner_id FROM exercises WHERE id = :id",
                new MapSqlParameterSource("id", exerciseId),
                (rs, rowNum) -> new ExerciseInfo(
                        rs.getObject("id", UUID.class),
                        rs.getString("name_es"),
                        rs.getBoolean("bodyweight"),
                        rs.getBoolean("is_custom"),
                        rs.getObject("owner_id", UUID.class)));
        return info.stream().findFirst();
    }

    private WorkoutSession hydrate(Row row) {
        List<SessionExercise> exercises = jdbc.query(
                """
                SELECT se.id, se.exercise_id, e.name_es, se.position, se.rest_seconds,
                       se.superset_group, se.notes
                FROM session_exercises se JOIN exercises e ON e.id = se.exercise_id
                WHERE se.session_id = :sessionId ORDER BY se.position
                """,
                new MapSqlParameterSource("sessionId", row.id()),
                (rs, rowNum) -> new SessionExercise(
                        rs.getObject("id", UUID.class),
                        rs.getObject("exercise_id", UUID.class),
                        rs.getString("name_es"),
                        rs.getInt("position"),
                        (Integer) rs.getObject("rest_seconds"),
                        (Integer) rs.getObject("superset_group"),
                        rs.getString("notes"),
                        List.of(),
                        List.of()));
        List<SessionExercise> withSets = exercises.stream()
                .map(exercise -> new SessionExercise(
                        exercise.id(),
                        exercise.exerciseId(),
                        exercise.exerciseName(),
                        exercise.position(),
                        exercise.restSeconds(),
                        exercise.supersetGroup(),
                        exercise.notes(),
                        sets(exercise.id()),
                        targetSets(exercise.id())))
                .toList();
        return new WorkoutSession(
                row.id(),
                row.name(),
                row.notes(),
                row.status(),
                row.startedAt(),
                row.finishedAt(),
                row.durationSeconds(),
                row.totalVolume(),
                withSets);
    }

    private List<SessionSet> sets(UUID entryId) {
        return jdbc.query("""
                SELECT id, position, set_type, reps, weight, rir, logged_at, notes
                FROM session_sets WHERE session_exercise_id = :id ORDER BY position
                """, new MapSqlParameterSource("id", entryId), (rs, rowNum) -> {
            SetType type = SetType.valueOf(rs.getString("set_type"));
            int reps = rs.getInt("reps");
            BigDecimal weight = rs.getBigDecimal("weight");
            return new SessionSet(
                    rs.getObject("id", UUID.class),
                    rs.getInt("position"),
                    type,
                    reps,
                    weight,
                    (Integer) rs.getObject("rir"),
                    SetMath.volume(type, weight, reps),
                    SetMath.e1rm(type, weight, reps),
                    instant(rs.getTimestamp("logged_at")),
                    rs.getString("notes"));
        });
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record Row(
            UUID id,
            String name,
            String notes,
            SessionStatus status,
            Instant startedAt,
            Instant finishedAt,
            Integer durationSeconds,
            BigDecimal totalVolume) {}

    private record SetRow(UUID id, int reps, BigDecimal weight, Instant loggedAt, SetType type) {}

    private record RebuildSet(UUID id, int reps, BigDecimal weight, Instant loggedAt, SetType type) {}

    private record ExerciseRow(UUID id, String name) {}

    private List<TargetSet> targetSets(UUID entryId) {
        return jdbc.query(
                """
                SELECT id, position, set_type, target_reps, target_weight, target_rir
                FROM session_target_sets WHERE session_exercise_id = :id ORDER BY position
                """,
                new MapSqlParameterSource("id", entryId),
                (rs, rowNum) -> new TargetSet(
                        rs.getObject("id", UUID.class),
                        rs.getInt("position"),
                        SetType.valueOf(rs.getString("set_type")),
                        rs.getInt("target_reps"),
                        rs.getBigDecimal("target_weight"),
                        (Integer) rs.getObject("target_rir")));
    }

    @Override
    public void updateSession(UUID sessionId, String name, String notes) {
        jdbc.update(
                "UPDATE workout_sessions SET name = coalesce(:name, name), notes = coalesce(:notes, notes), "
                        + "updated_at = now() WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", sessionId)
                        .addValue("name", name)
                        .addValue("notes", notes));
    }

    @Override
    public void updateExercise(UUID entryId, Integer restSeconds, Integer supersetGroup, String notes) {
        jdbc.update(
                """
                UPDATE session_exercises SET
                    rest_seconds = coalesce(:rest, rest_seconds),
                    superset_group = coalesce(:superset, superset_group),
                    notes = coalesce(:notes, notes)
                WHERE id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("id", entryId)
                        .addValue("rest", restSeconds)
                        .addValue("superset", supersetGroup)
                        .addValue("notes", notes));
    }

    @Override
    public void deleteExercise(UUID entryId) {
        List<UUID> sessions = jdbc.query(
                "SELECT session_id FROM session_exercises WHERE id = :id",
                new MapSqlParameterSource("id", entryId),
                (rs, rowNum) -> rs.getObject("session_id", UUID.class));
        if (sessions.isEmpty()) {
            return;
        }
        UUID sessionId = sessions.getFirst();
        jdbc.update("DELETE FROM session_exercises WHERE id = :id", new MapSqlParameterSource("id", entryId));
        renumberExercises(sessionId);
    }

    @Override
    public void reorderExercises(UUID sessionId, List<UUID> orderedEntryIds) {
        jdbc.update(
                "UPDATE session_exercises SET position = position + 1000 WHERE session_id = :sessionId",
                new MapSqlParameterSource("sessionId", sessionId));
        for (int i = 0; i < orderedEntryIds.size(); i++) {
            jdbc.update(
                    "UPDATE session_exercises SET position = :position WHERE id = :id AND session_id = :sessionId",
                    new MapSqlParameterSource()
                            .addValue("position", i)
                            .addValue("id", orderedEntryIds.get(i))
                            .addValue("sessionId", sessionId));
        }
    }

    @Override
    public void setSupersetGroup(UUID sessionId, List<UUID> entryIds, Integer group) {
        for (UUID entryId : entryIds) {
            jdbc.update(
                    "UPDATE session_exercises SET superset_group = :group "
                            + "WHERE id = :id AND session_id = :sessionId",
                    new MapSqlParameterSource()
                            .addValue("group", group)
                            .addValue("id", entryId)
                            .addValue("sessionId", sessionId));
        }
    }

    @Override
    public void updateSet(UUID setId, SetType type, int reps, BigDecimal weight, Integer rir, String notes) {
        jdbc.update(
                """
                UPDATE session_sets SET set_type = CAST(:type AS set_type), reps = :reps,
                    weight = :weight, rir = :rir, notes = coalesce(:notes, notes),
                    updated_at = now() WHERE id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("id", setId)
                        .addValue("type", type.name())
                        .addValue("reps", reps)
                        .addValue("weight", weight)
                        .addValue("rir", rir)
                        .addValue("notes", notes));
    }

    @Override
    public void deleteSet(UUID setId) {
        List<UUID> entries = jdbc.query(
                "SELECT session_exercise_id FROM session_sets WHERE id = :id",
                new MapSqlParameterSource("id", setId),
                (rs, rowNum) -> rs.getObject("session_exercise_id", UUID.class));
        if (entries.isEmpty()) {
            return;
        }
        UUID entryId = entries.getFirst();
        jdbc.update("DELETE FROM session_sets WHERE id = :id", new MapSqlParameterSource("id", setId));
        jdbc.update(
                "UPDATE session_sets SET position = position + 1000 WHERE session_exercise_id = :id",
                new MapSqlParameterSource("id", entryId));
        jdbc.update("""
                WITH ranked AS (
                    SELECT id, row_number() OVER (ORDER BY position) - 1 AS new_position
                    FROM session_sets WHERE session_exercise_id = :id
                )
                UPDATE session_sets s SET position = ranked.new_position
                FROM ranked WHERE s.id = ranked.id
                """, new MapSqlParameterSource("id", entryId));
    }

    @Override
    public void updateTotalVolume(UUID sessionId, BigDecimal volume) {
        jdbc.update(
                "UPDATE workout_sessions SET total_volume = :volume, updated_at = now() WHERE id = :id",
                new MapSqlParameterSource().addValue("id", sessionId).addValue("volume", volume));
    }

    @Override
    public void rebuildPersonalRecords(UUID userId, UUID exerciseId) {
        jdbc.update(
                "DELETE FROM personal_records WHERE user_id = :userId AND exercise_id = :exerciseId",
                new MapSqlParameterSource().addValue("userId", userId).addValue("exerciseId", exerciseId));
        List<RebuildSet> sets = jdbc.query(
                """
                SELECT ss.id, ss.reps, ss.weight, ss.logged_at, ss.set_type
                FROM session_sets ss
                JOIN session_exercises se ON se.id = ss.session_exercise_id
                JOIN workout_sessions ws ON ws.id = se.session_id
                WHERE ws.user_id = :userId AND se.exercise_id = :exerciseId
                  AND ws.status IN ('FINISHED', 'AUTO_CLOSED')
                ORDER BY ss.logged_at, ss.id
                """,
                new MapSqlParameterSource().addValue("userId", userId).addValue("exerciseId", exerciseId),
                (rs, rowNum) -> new RebuildSet(
                        rs.getObject("id", UUID.class),
                        rs.getInt("reps"),
                        rs.getBigDecimal("weight"),
                        instant(rs.getTimestamp("logged_at")),
                        SetType.valueOf(rs.getString("set_type"))));
        String name = jdbc.queryForObject(
                "SELECT name_es FROM exercises WHERE id = :id",
                new MapSqlParameterSource("id", exerciseId),
                String.class);
        BigDecimal maxWeight = null;
        BigDecimal maxE1rm = null;
        for (RebuildSet set : sets) {
            if (set.type() == SetType.WARMUP
                    || set.weight() == null
                    || set.weight().signum() <= 0) {
                continue;
            }
            if (maxWeight == null || set.weight().compareTo(maxWeight) > 0) {
                maxWeight = set.weight();
                insertRecord(userId, exerciseId, name, PrType.MAX_WEIGHT, maxWeight, set.loggedAt(), set.id());
            }
            if (set.reps() <= 12) {
                BigDecimal e1rm = SetMath.e1rm(set.type(), set.weight(), set.reps());
                if (e1rm != null && (maxE1rm == null || e1rm.compareTo(maxE1rm) > 0)) {
                    maxE1rm = e1rm;
                    insertRecord(userId, exerciseId, name, PrType.MAX_E1RM, maxE1rm, set.loggedAt(), set.id());
                }
            }
        }
    }

    private void insertRecord(
            UUID userId, UUID exerciseId, String name, PrType type, BigDecimal value, Instant achievedAt, UUID setId) {
        jdbc.update(
                """
                INSERT INTO personal_records
                    (id, user_id, exercise_id, pr_type, value, achieved_at, session_set_id)
                VALUES (:id, :userId, :exerciseId, CAST(:type AS pr_type), :value, :achievedAt, :setId)
                """,
                new MapSqlParameterSource()
                        .addValue("id", UuidV7.generate())
                        .addValue("userId", userId)
                        .addValue("exerciseId", exerciseId)
                        .addValue("type", type.name())
                        .addValue("value", value)
                        .addValue("achievedAt", Timestamp.from(achievedAt))
                        .addValue("setId", setId));
    }

    @Override
    public List<PreviousSession> previousSessions(UUID userId, UUID exerciseId, int limit) {
        List<PreviousSession> result = new ArrayList<>();
        List<UUID> sessionIds = jdbc.query(
                """
                SELECT DISTINCT ws.id, ws.started_at FROM workout_sessions ws
                JOIN session_exercises se ON se.session_id = ws.id
                JOIN session_sets ss ON ss.session_exercise_id = se.id
                WHERE ws.user_id = :userId AND se.exercise_id = :exerciseId
                  AND ws.status IN ('FINISHED', 'AUTO_CLOSED')
                ORDER BY ws.started_at DESC LIMIT :limit
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("exerciseId", exerciseId)
                        .addValue("limit", Math.max(1, Math.min(limit, 10))),
                (rs, rowNum) -> rs.getObject("id", UUID.class));
        for (UUID sessionId : sessionIds) {
            Instant startedAt = jdbc.queryForObject(
                            "SELECT started_at FROM workout_sessions WHERE id = :id",
                            new MapSqlParameterSource("id", sessionId),
                            Timestamp.class)
                    .toInstant();
            List<PreviousSet> sets = jdbc.query(
                    """
                    SELECT ss.reps, ss.weight, ss.rir, ss.set_type, ss.logged_at
                    FROM session_sets ss
                    JOIN session_exercises se ON se.id = ss.session_exercise_id
                    WHERE se.session_id = :sessionId AND se.exercise_id = :exerciseId
                    ORDER BY se.position, ss.position
                    """,
                    new MapSqlParameterSource().addValue("sessionId", sessionId).addValue("exerciseId", exerciseId),
                    (rs, rowNum) -> new PreviousSet(
                            rs.getInt("reps"),
                            rs.getBigDecimal("weight"),
                            (Integer) rs.getObject("rir"),
                            SetType.valueOf(rs.getString("set_type")),
                            instant(rs.getTimestamp("logged_at"))));
            result.add(new PreviousSession(sessionId, startedAt, sets));
        }
        return result;
    }

    @Override
    public List<StaleSession> staleActive(Instant threshold) {
        return jdbc.query(
                """
                SELECT ws.id, ws.user_id, u.email, ws.started_at,
                    (SELECT max(ss.logged_at) FROM session_exercises se
                     JOIN session_sets ss ON ss.session_exercise_id = se.id
                     WHERE se.session_id = ws.id) AS last_set_at
                FROM workout_sessions ws JOIN users u ON u.id = ws.user_id
                WHERE ws.status = 'ACTIVE' AND ws.started_at < :threshold
                """,
                new MapSqlParameterSource("threshold", Timestamp.from(threshold)),
                (rs, rowNum) -> new StaleSession(
                        rs.getObject("id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("email"),
                        instant(rs.getTimestamp("started_at")),
                        instant(rs.getTimestamp("last_set_at"))));
    }

    @Override
    public void autoClose(UUID sessionId, Instant finishedAt, int durationSeconds) {
        jdbc.update(
                """
                UPDATE workout_sessions SET status = 'AUTO_CLOSED', finished_at = :finishedAt,
                    duration_seconds = :duration, updated_at = now() WHERE id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("id", sessionId)
                        .addValue("finishedAt", Timestamp.from(finishedAt))
                        .addValue("duration", durationSeconds));
    }

    private void renumberExercises(UUID sessionId) {
        jdbc.update(
                "UPDATE session_exercises SET position = position + 1000 WHERE session_id = :sessionId",
                new MapSqlParameterSource("sessionId", sessionId));
        jdbc.update("""
                WITH ranked AS (
                    SELECT id, row_number() OVER (ORDER BY position) - 1 AS new_position
                    FROM session_exercises WHERE session_id = :sessionId
                )
                UPDATE session_exercises s SET position = ranked.new_position
                FROM ranked WHERE s.id = ranked.id
                """, new MapSqlParameterSource("sessionId", sessionId));
    }
}
