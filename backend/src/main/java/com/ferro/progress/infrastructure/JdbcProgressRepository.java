package com.ferro.progress.infrastructure;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ferro.progress.application.port.ProgressRepository;
import com.ferro.progress.domain.PersonalRecordGroup;
import com.ferro.progress.domain.ProgressSummary;
import com.ferro.progress.domain.SessionDetail;
import com.ferro.progress.domain.SessionPage;
import com.ferro.progress.domain.SessionSummary;
import com.ferro.progress.domain.VolumePoint;

@Repository
public class JdbcProgressRepository implements ProgressRepository {

    private static final String COMPLETED = "ws.status IN ('FINISHED', 'AUTO_CLOSED')";

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcProgressRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public SessionPage findSessions(UUID userId, Instant from, Instant to, UUID exerciseId, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("from", from == null ? null : Timestamp.from(from))
                .addValue("to", to == null ? null : Timestamp.from(to))
                .addValue("exerciseId", exerciseId)
                .addValue("limit", size)
                .addValue("offset", page * size);
        String filter = exerciseId == null ? "" : """
                  AND EXISTS (SELECT 1 FROM session_exercises fse
                      JOIN session_sets fss ON fss.session_exercise_id = fse.id
                      WHERE fse.session_id = ws.id AND fse.exercise_id = :exerciseId)
                  """;
        Long total = jdbc.queryForObject("""
                SELECT count(*) FROM workout_sessions ws
                WHERE ws.user_id = :userId AND %s
                  AND ws.finished_at >= coalesce(:from, '-infinity'::timestamptz)
                  AND ws.finished_at < coalesce(:to, 'infinity'::timestamptz)
                  %s
                """.formatted(COMPLETED, filter), params, Long.class);
        List<SessionSummary> content = jdbc.query(
                """
                SELECT ws.id, ws.name, ws.started_at, ws.finished_at, ws.duration_seconds,
                       ws.total_volume, ws.status,
                       count(DISTINCT se.id) AS exercise_count,
                       count(ss.id) AS set_count,
                       EXISTS (SELECT 1 FROM personal_records pr
                           JOIN session_sets pss ON pss.id = pr.session_set_id
                           JOIN session_exercises pse ON pse.id = pss.session_exercise_id
                           WHERE pse.session_id = ws.id) AS has_pr
                FROM workout_sessions ws
                LEFT JOIN session_exercises se ON se.session_id = ws.id
                LEFT JOIN session_sets ss ON ss.session_exercise_id = se.id
                WHERE ws.user_id = :userId AND %s
                  AND ws.finished_at >= coalesce(:from, '-infinity'::timestamptz)
                  AND ws.finished_at < coalesce(:to, 'infinity'::timestamptz)
                  %s
                GROUP BY ws.id
                ORDER BY ws.finished_at DESC
                LIMIT :limit OFFSET :offset
                """.formatted(COMPLETED, filter),
                params,
                (rs, rowNum) -> new SessionSummary(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        instant(rs.getTimestamp("started_at")),
                        instant(rs.getTimestamp("finished_at")),
                        rs.getInt("duration_seconds"),
                        rs.getBigDecimal("total_volume"),
                        rs.getInt("exercise_count"),
                        rs.getInt("set_count"),
                        rs.getBoolean("has_pr"),
                        rs.getString("status")));
        long totalElements = total == null ? 0 : total;
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new SessionPage(content, page, size, totalElements, totalPages);
    }

    @Override
    public Optional<SessionDetail> findSession(UUID userId, UUID sessionId) {
        List<DetailRow> rows = jdbc.query(
                """
                SELECT id, name, notes, started_at, finished_at, duration_seconds, total_volume, status
                FROM workout_sessions
                WHERE id = :sessionId AND user_id = :userId AND %s
                """.formatted(COMPLETED.replace("ws.", "")),
                new MapSqlParameterSource().addValue("sessionId", sessionId).addValue("userId", userId),
                (rs, rowNum) -> new DetailRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("notes"),
                        instant(rs.getTimestamp("started_at")),
                        instant(rs.getTimestamp("finished_at")),
                        rs.getInt("duration_seconds"),
                        rs.getBigDecimal("total_volume"),
                        rs.getString("status")));
        return rows.stream()
                .findFirst()
                .map(row -> new SessionDetail(
                        row.id(),
                        row.name(),
                        row.notes(),
                        row.startedAt(),
                        row.finishedAt(),
                        row.durationSeconds(),
                        row.totalVolume(),
                        row.status(),
                        exercises(row.id())));
    }

    private List<SessionDetail.ExerciseDetail> exercises(UUID sessionId) {
        List<ExerciseRow> rows = jdbc.query(
                """
                SELECT se.id, se.exercise_id, e.name_es, se.position, se.notes
                FROM session_exercises se JOIN exercises e ON e.id = se.exercise_id
                WHERE se.session_id = :sessionId ORDER BY se.position
                """,
                new MapSqlParameterSource("sessionId", sessionId),
                (rs, rowNum) -> new ExerciseRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("exercise_id", UUID.class),
                        rs.getString("name_es"),
                        rs.getInt("position"),
                        rs.getString("notes")));
        List<SessionDetail.ExerciseDetail> result = new ArrayList<>();
        for (ExerciseRow row : rows) {
            List<SessionDetail.SetDetail> sets = jdbc.query(
                    """
                    SELECT id, position, set_type, reps, weight, rir, notes, logged_at
                    FROM session_sets WHERE session_exercise_id = :id ORDER BY position
                    """,
                    new MapSqlParameterSource("id", row.id()),
                    (rs, rowNum) -> new SessionDetail.SetDetail(
                            rs.getObject("id", UUID.class),
                            rs.getInt("position"),
                            rs.getString("set_type"),
                            rs.getInt("reps"),
                            rs.getBigDecimal("weight"),
                            (Integer) rs.getObject("rir"),
                            volume(rs.getString("set_type"), rs.getInt("reps"), rs.getBigDecimal("weight")),
                            e1rm(rs.getString("set_type"), rs.getInt("reps"), rs.getBigDecimal("weight")),
                            rs.getString("notes"),
                            instant(rs.getTimestamp("logged_at"))));
            BigDecimal volume =
                    sets.stream().map(SessionDetail.SetDetail::volume).reduce(BigDecimal.ZERO, BigDecimal::add);
            List<SessionDetail.PrAchieved> prs = jdbc.query(
                    """
                    SELECT pr.pr_type, pr.value
                    FROM personal_records pr
                    JOIN session_sets ss ON ss.id = pr.session_set_id
                    WHERE ss.session_exercise_id = :id
                    ORDER BY pr.pr_type, pr.value DESC
                    """,
                    new MapSqlParameterSource("id", row.id()),
                    (rs, rowNum) -> new SessionDetail.PrAchieved(rs.getString("pr_type"), rs.getBigDecimal("value")));
            result.add(new SessionDetail.ExerciseDetail(
                    row.id(), row.exerciseId(), row.name(), row.position(), volume, row.notes(), sets, prs));
        }
        return result;
    }

    @Override
    public List<PersonalRecordGroup> personalRecords(UUID userId, UUID exerciseId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        String filter = "";
        if (exerciseId != null) {
            filter = " AND pr.exercise_id = :exerciseId ";
            params.addValue("exerciseId", exerciseId);
        }
        List<PrRow> rows = jdbc.query(
                """
                SELECT pr.exercise_id, e.name_es, pr.pr_type, pr.value, pr.achieved_at
                FROM personal_records pr JOIN exercises e ON e.id = pr.exercise_id
                WHERE pr.user_id = :userId %s
                ORDER BY e.name_es, pr.pr_type, pr.value DESC, pr.achieved_at DESC
                """.formatted(filter),
                params,
                (rs, rowNum) -> new PrRow(
                        rs.getObject("exercise_id", UUID.class),
                        rs.getString("name_es"),
                        rs.getString("pr_type"),
                        rs.getBigDecimal("value"),
                        instant(rs.getTimestamp("achieved_at"))));
        Map<UUID, List<PrRow>> grouped = new LinkedHashMap<>();
        for (PrRow row : rows) {
            grouped.computeIfAbsent(row.exerciseId(), key -> new ArrayList<>()).add(row);
        }
        List<PersonalRecordGroup> result = new ArrayList<>();
        for (List<PrRow> group : grouped.values()) {
            PrRow nameRow = group.getFirst();
            Map<String, PersonalRecordGroup.RecordEntry> current = new LinkedHashMap<>();
            List<PersonalRecordGroup.RecordEntry> history = new ArrayList<>();
            for (PrRow row : group) {
                PersonalRecordGroup.RecordEntry entry =
                        new PersonalRecordGroup.RecordEntry(row.type(), row.value(), row.achievedAt());
                if (!current.containsKey(row.type())) {
                    current.put(row.type(), entry);
                } else {
                    history.add(entry);
                }
            }
            result.add(new PersonalRecordGroup(
                    nameRow.exerciseId(), nameRow.exerciseName(), new ArrayList<>(current.values()), history));
        }
        result.sort(Comparator.comparing((PersonalRecordGroup group) -> group.current().stream()
                        .map(PersonalRecordGroup.RecordEntry::achievedAt)
                        .max(Comparator.naturalOrder())
                        .orElse(Instant.EPOCH))
                .reversed());
        return result;
    }

    @Override
    public String timeZone(UUID userId) {
        String zone = jdbc.queryForObject(
                "SELECT time_zone FROM users WHERE id = :userId",
                new MapSqlParameterSource("userId", userId),
                String.class);
        return zone == null || zone.isBlank() ? "Europe/Madrid" : zone;
    }

    @Override
    public List<LocalDate> finishedDates(UUID userId, String zone) {
        return jdbc.query(
                """
                SELECT DISTINCT (ws.finished_at AT TIME ZONE :zone)::date AS day
                FROM workout_sessions ws
                WHERE ws.user_id = :userId AND %s
                ORDER BY day DESC
                """.formatted(COMPLETED),
                new MapSqlParameterSource().addValue("userId", userId).addValue("zone", zone),
                (rs, rowNum) -> rs.getObject("day", LocalDate.class));
    }

    @Override
    public List<ProgressSummary.WeekPoint> weeklyVolume(UUID userId, Instant from, Instant to, String zone) {
        return jdbc.query(
                """
                SELECT date_trunc('week', ws.finished_at AT TIME ZONE :zone) AS week,
                       coalesce(sum(CASE WHEN ss.set_type <> 'WARMUP' AND ss.weight > 0
                           THEN ss.weight * ss.reps ELSE 0 END), 0) AS value
                FROM workout_sessions ws
                JOIN session_exercises se ON se.session_id = ws.id
                JOIN session_sets ss ON ss.session_exercise_id = se.id
                WHERE ws.user_id = :userId AND %s AND ws.finished_at >= :from AND ws.finished_at < :to
                GROUP BY week ORDER BY week
                """.formatted(COMPLETED),
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("zone", zone)
                        .addValue("from", Timestamp.from(from))
                        .addValue("to", Timestamp.from(to)),
                (rs, rowNum) ->
                        new ProgressSummary.WeekPoint(instant(rs.getTimestamp("week")), rs.getBigDecimal("value")));
    }

    @Override
    public List<ProgressSummary.WeekPoint> weeklySessions(UUID userId, Instant from, Instant to, String zone) {
        return jdbc.query(
                """
                SELECT date_trunc('week', ws.finished_at AT TIME ZONE :zone) AS week,
                       count(DISTINCT ws.id) AS value
                FROM workout_sessions ws
                WHERE ws.user_id = :userId AND %s AND ws.finished_at >= :from AND ws.finished_at < :to
                GROUP BY week ORDER BY week
                """.formatted(COMPLETED),
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("zone", zone)
                        .addValue("from", Timestamp.from(from))
                        .addValue("to", Timestamp.from(to)),
                (rs, rowNum) -> new ProgressSummary.WeekPoint(
                        instant(rs.getTimestamp("week")), BigDecimal.valueOf(rs.getLong("value"))));
    }

    @Override
    public ProgressSummary.PeriodTotals totals(UUID userId, Instant from, Instant to) {
        return jdbc.queryForObject(
                """
                SELECT coalesce(sum(CASE WHEN ss.set_type <> 'WARMUP' AND ss.weight > 0
                           THEN ss.weight * ss.reps ELSE 0 END), 0) AS volume,
                       count(DISTINCT ws.id) AS sessions,
                       count(ss.id) AS sets
                FROM workout_sessions ws
                LEFT JOIN session_exercises se ON se.session_id = ws.id
                LEFT JOIN session_sets ss ON ss.session_exercise_id = se.id
                WHERE ws.user_id = :userId AND %s AND ws.finished_at >= :from AND ws.finished_at < :to
                """.formatted(COMPLETED),
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("from", Timestamp.from(from))
                        .addValue("to", Timestamp.from(to)),
                (rs, rowNum) -> new ProgressSummary.PeriodTotals(
                        rs.getBigDecimal("volume"), rs.getLong("sessions"), rs.getLong("sets")));
    }

    @Override
    public List<ProgressSummary.MuscleSlice> muscleDistribution(UUID userId, Instant from, Instant to) {
        return jdbc.query(
                """
                SELECT e.muscle_group,
                       coalesce(sum(CASE WHEN ss.set_type <> 'WARMUP' AND ss.weight > 0
                           THEN ss.weight * ss.reps ELSE 0 END), 0) AS volume
                FROM workout_sessions ws
                JOIN session_exercises se ON se.session_id = ws.id
                JOIN session_sets ss ON ss.session_exercise_id = se.id
                JOIN exercises e ON e.id = se.exercise_id
                WHERE ws.user_id = :userId AND %s AND ws.finished_at >= :from AND ws.finished_at < :to
                GROUP BY e.muscle_group ORDER BY volume DESC
                """.formatted(COMPLETED),
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("from", Timestamp.from(from))
                        .addValue("to", Timestamp.from(to)),
                (rs, rowNum) ->
                        new ProgressSummary.MuscleSlice(rs.getString("muscle_group"), rs.getBigDecimal("volume")));
    }

    @Override
    public List<VolumePoint> volume(UUID userId, Instant from, Instant to, String zone, String granularity) {
        String trunc = switch (granularity) {
            case "day" -> "day";
            case "month" -> "month";
            default -> "week";
        };
        return jdbc.query(
                """
                SELECT date_trunc('%s', ws.finished_at AT TIME ZONE :zone) AS bucket,
                       coalesce(sum(CASE WHEN ss.set_type <> 'WARMUP' AND ss.weight > 0
                           THEN ss.weight * ss.reps ELSE 0 END), 0) AS volume
                FROM workout_sessions ws
                JOIN session_exercises se ON se.session_id = ws.id
                JOIN session_sets ss ON ss.session_exercise_id = se.id
                WHERE ws.user_id = :userId AND %s AND ws.finished_at >= :from AND ws.finished_at < :to
                GROUP BY bucket ORDER BY bucket
                """.formatted(trunc, COMPLETED),
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("zone", zone)
                        .addValue("from", Timestamp.from(from))
                        .addValue("to", Timestamp.from(to)),
                (rs, rowNum) -> new VolumePoint(instant(rs.getTimestamp("bucket")), rs.getBigDecimal("volume")));
    }

    private static BigDecimal volume(String type, int reps, BigDecimal weight) {
        if ("WARMUP".equals(type) || weight == null || weight.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return weight.multiply(BigDecimal.valueOf(reps));
    }

    private static BigDecimal e1rm(String type, int reps, BigDecimal weight) {
        if ("WARMUP".equals(type) || weight == null || weight.signum() <= 0 || reps > 12) {
            return null;
        }
        return weight.multiply(BigDecimal.valueOf(1 + reps / 30.0));
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record DetailRow(
            UUID id,
            String name,
            String notes,
            Instant startedAt,
            Instant finishedAt,
            int durationSeconds,
            BigDecimal totalVolume,
            String status) {}

    private record ExerciseRow(UUID id, UUID exerciseId, String name, int position, String notes) {}

    private record PrRow(UUID exerciseId, String exerciseName, String type, BigDecimal value, Instant achievedAt) {}
}
