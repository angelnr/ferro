package com.ferro.catalog.infrastructure;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ferro.catalog.application.port.ExerciseStatsRepository;
import com.ferro.catalog.domain.ExerciseStats;

@Repository
public class JdbcExerciseStatsRepository implements ExerciseStatsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcExerciseStatsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ExerciseStats statsFor(UUID userId, UUID exerciseId) {
        MapSqlParameterSource params =
                new MapSqlParameterSource().addValue("userId", userId).addValue("exerciseId", exerciseId);

        Long sessions = jdbc.queryForObject("""
                SELECT count(DISTINCT ws.id) FROM workout_sessions ws
                JOIN session_exercises se ON se.session_id = ws.id
                JOIN session_sets ss ON ss.session_exercise_id = se.id
                WHERE ws.user_id = :userId AND se.exercise_id = :exerciseId
                  AND ws.status IN ('FINISHED', 'AUTO_CLOSED')
                """, params, Long.class);
        BigDecimal volume = jdbc.queryForObject("""
                SELECT coalesce(sum(ss.reps * ss.weight), 0) FROM workout_sessions ws
                JOIN session_exercises se ON se.session_id = ws.id
                JOIN session_sets ss ON ss.session_exercise_id = se.id
                WHERE ws.user_id = :userId AND se.exercise_id = :exerciseId
                  AND ws.status IN ('FINISHED', 'AUTO_CLOSED')
                """, params, BigDecimal.class);
        BigDecimal bestE1rm = jdbc.queryForObject("""
                SELECT max(ss.weight * (1 + ss.reps / 30.0)) FROM workout_sessions ws
                JOIN session_exercises se ON se.session_id = ws.id
                JOIN session_sets ss ON ss.session_exercise_id = se.id
                WHERE ws.user_id = :userId AND se.exercise_id = :exerciseId
                  AND ss.weight > 0 AND ss.reps <= 12 AND ws.status IN ('FINISHED', 'AUTO_CLOSED')
                """, params, BigDecimal.class);

        List<ExerciseStats.PersonalRecord> records = jdbc.query(
                """
                SELECT DISTINCT ON (pr_type) pr_type, value, achieved_at
                FROM personal_records
                WHERE user_id = :userId AND exercise_id = :exerciseId
                ORDER BY pr_type, value DESC, achieved_at DESC
                """,
                params,
                (rs, rowNum) -> new ExerciseStats.PersonalRecord(
                        rs.getString("pr_type"), rs.getBigDecimal("value"), instant(rs.getTimestamp("achieved_at"))));

        List<ExerciseStats.E1rmPoint> series = jdbc.query(
                """
                SELECT date_trunc('week', ws.started_at) AS week,
                       max(ss.weight * (1 + ss.reps / 30.0)) AS e1rm
                FROM workout_sessions ws
                JOIN session_exercises se ON se.session_id = ws.id
                JOIN session_sets ss ON ss.session_exercise_id = se.id
                WHERE ws.user_id = :userId AND se.exercise_id = :exerciseId
                  AND ss.weight > 0 AND ss.reps <= 12 AND ws.status IN ('FINISHED', 'AUTO_CLOSED')
                  AND ws.started_at >= now() - interval '52 weeks'
                GROUP BY week ORDER BY week
                """,
                params,
                (rs, rowNum) ->
                        new ExerciseStats.E1rmPoint(instant(rs.getTimestamp("week")), rs.getBigDecimal("e1rm")));

        return new ExerciseStats(
                records, bestE1rm, volume == null ? BigDecimal.ZERO : volume, sessions == null ? 0 : sessions, series);
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
