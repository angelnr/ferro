package com.ferro.identity.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Export completo de los datos del usuario (RGPD, RNF-22). */
@Service
public class UserDataExportService {

    private final NamedParameterJdbcTemplate jdbc;

    public UserDataExportService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportedAt", java.time.Instant.now());
        data.put("profile", jdbc.queryForMap("""
                SELECT id, email::text AS email, display_name, status, locale, unit_system, theme, time_zone,
                       default_rest_seconds, timer_sound, email_weekly_summary, email_verified_at,
                       health_consent_at, created_at, updated_at
                FROM users WHERE id = :userId
                """, params));
        data.put(
                "routines",
                jdbc.queryForList(
                        "SELECT id, name, notes, created_at, updated_at FROM routines WHERE user_id = :userId",
                        params));
        data.put("routineExercises", jdbc.queryForList("""
                SELECT re.id, re.routine_id, re.exercise_id, re.position, re.rest_seconds,
                       re.superset_group, re.notes
                FROM routine_exercises re JOIN routines r ON r.id = re.routine_id
                WHERE r.user_id = :userId
                """, params));
        data.put("routineSets", jdbc.queryForList("""
                SELECT rs.id, rs.routine_exercise_id, rs.position, rs.set_type,
                       rs.target_reps, rs.target_weight, rs.target_rir
                FROM routine_sets rs
                JOIN routine_exercises re ON re.id = rs.routine_exercise_id
                JOIN routines r ON r.id = re.routine_id
                WHERE r.user_id = :userId
                """, params));
        data.put("workoutSessions", jdbc.queryForList("""
                SELECT id, routine_id, name, status, started_at, finished_at, duration_seconds,
                       total_volume, notes
                FROM workout_sessions WHERE user_id = :userId
                """, params));
        data.put("sessionExercises", jdbc.queryForList("""
                SELECT se.id, se.session_id, se.exercise_id, se.position, se.rest_seconds,
                       se.superset_group, se.notes
                FROM session_exercises se JOIN workout_sessions ws ON ws.id = se.session_id
                WHERE ws.user_id = :userId
                """, params));
        data.put("sessionSets", jdbc.queryForList("""
                SELECT ss.id, ss.session_exercise_id, ss.position, ss.set_type, ss.reps,
                       ss.weight, ss.rir, ss.notes, ss.logged_at
                FROM session_sets ss
                JOIN session_exercises se ON se.id = ss.session_exercise_id
                JOIN workout_sessions ws ON ws.id = se.session_id
                WHERE ws.user_id = :userId
                """, params));
        data.put("personalRecords", jdbc.queryForList("""
                SELECT id, exercise_id, pr_type, value, achieved_at, session_set_id
                FROM personal_records WHERE user_id = :userId
                """, params));
        data.put("bodyMeasurements", jdbc.queryForList("""
                SELECT id, measured_at, weight, chest, waist, hip, arm, thigh, notes
                FROM body_measurements WHERE user_id = :userId
                """, params));
        return data;
    }
}
