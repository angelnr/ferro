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
import com.ferro.training.domain.SessionAlreadyActiveException;
import com.ferro.training.domain.SessionExercise;
import com.ferro.training.domain.SessionNotFoundException;
import com.ferro.training.domain.SessionSet;
import com.ferro.training.domain.SessionStatus;
import com.ferro.training.domain.SetMath;
import com.ferro.training.domain.SetType;
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
                SELECT id, name, status, started_at, finished_at, duration_seconds, total_volume
                FROM workout_sessions WHERE id = :sessionId AND user_id = :userId
                """,
                new MapSqlParameterSource("sessionId", sessionId).addValue("userId", userId),
                (rs, rowNum) -> new Row(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        SessionStatus.valueOf(rs.getString("status")),
                        instant(rs.getTimestamp("started_at")),
                        instant(rs.getTimestamp("finished_at")),
                        (Integer) rs.getObject("duration_seconds"),
                        rs.getBigDecimal("total_volume")));
        return rows.stream().findFirst().map(row -> hydrate(row));
    }

    @Override
    public WorkoutSession create(UUID userId, UUID sessionId, String name, Instant now) {
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
                    INSERT INTO workout_sessions (id, user_id, name, status, started_at, total_volume)
                    VALUES (:id, :userId, :name, 'ACTIVE', :now, 0)
                    """,
                    new MapSqlParameterSource()
                            .addValue("id", sessionId)
                            .addValue("userId", userId)
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
    public void addExercise(UUID sessionId, UUID entryId, UUID exerciseId, int position) {
        jdbc.update(
                """
                INSERT INTO session_exercises (id, session_id, exercise_id, position)
                VALUES (:id, :sessionId, :exerciseId, :position)
                """,
                new MapSqlParameterSource()
                        .addValue("id", entryId)
                        .addValue("sessionId", sessionId)
                        .addValue("exerciseId", exerciseId)
                        .addValue("position", position));
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
            Instant loggedAt) {
        Integer position = jdbc.queryForObject(
                "SELECT coalesce(max(position), -1) + 1 FROM session_sets WHERE session_exercise_id = :id",
                new MapSqlParameterSource("id", entryId),
                Integer.class);
        int next = position == null ? 0 : position;
        jdbc.update(
                """
                INSERT INTO session_sets
                    (id, session_exercise_id, position, set_type, reps, weight, rir, logged_at)
                VALUES (:id, :entryId, :position, CAST(:type AS set_type), :reps, :weight, :rir, :loggedAt)
                """,
                new MapSqlParameterSource()
                        .addValue("id", setId)
                        .addValue("entryId", entryId)
                        .addValue("position", next)
                        .addValue("type", setType.name())
                        .addValue("reps", reps)
                        .addValue("weight", weight)
                        .addValue("rir", rir)
                        .addValue("loggedAt", Timestamp.from(loggedAt)));
        return new SessionSet(setId, next, setType, reps, weight, rir, volume, e1rm, loggedAt);
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
                        sets(exercise.id())))
                .toList();
        return new WorkoutSession(
                row.id(),
                row.name(),
                row.status(),
                row.startedAt(),
                row.finishedAt(),
                row.durationSeconds(),
                row.totalVolume(),
                withSets);
    }

    private List<SessionSet> sets(UUID entryId) {
        return jdbc.query("""
                SELECT id, position, set_type, reps, weight, rir, logged_at
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
                    instant(rs.getTimestamp("logged_at")));
        });
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record Row(
            UUID id,
            String name,
            SessionStatus status,
            Instant startedAt,
            Instant finishedAt,
            Integer durationSeconds,
            BigDecimal totalVolume) {}

    private record SetRow(UUID id, int reps, BigDecimal weight, Instant loggedAt, SetType type) {}

    private record ExerciseRow(UUID id, String name) {}
}
