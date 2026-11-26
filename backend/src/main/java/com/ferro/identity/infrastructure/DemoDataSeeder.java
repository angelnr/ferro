package com.ferro.identity.infrastructure;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.ferro.identity.application.port.PasswordHasher;
import com.ferro.shared.domain.UuidV7;

/**
 * Cuenta demo y 12 semanas de datos sintéticos (RF-087).
 *
 * <p>Solo se activa con {@code ferro.demo.enabled=true}; nunca en CI normal.
 */
@Component
@ConditionalOnProperty(name = "ferro.demo.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final String DEMO_EMAIL = "demo@ferro.app";
    private static final String DEMO_PASSWORD = "FerroDemo26";
    private static final String[] EXERCISES = {
        "Press de banca con barra", "Sentadilla trasera con barra", "Peso muerto rumano con barra"
    };

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final PasswordHasher passwordHasher;

    public DemoDataSeeder(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc, PasswordHasher passwordHasher) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer existing = jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, DEMO_EMAIL);
        if (existing != null && existing > 0) {
            return;
        }
        UUID userId = UuidV7.generate();
        Instant now = Instant.now();
        jdbc.update(
                """
                INSERT INTO users
                    (id, email, password_hash, display_name, status, locale, unit_system, theme,
                     time_zone, default_rest_seconds, timer_sound, email_verified_at,
                     health_consent_at, created_at, updated_at)
                VALUES (?, ?, ?, 'Ferro Demo', 'ACTIVE', 'es', 'KG', 'DARK',
                        'Europe/Madrid', 90, true, ?, ?, ?, ?)
                """,
                userId,
                DEMO_EMAIL,
                passwordHasher.hash(DEMO_PASSWORD),
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now));

        List<UUID> exerciseIds = exerciseIds();
        if (exerciseIds.size() < EXERCISES.length) {
            return;
        }
        List<UUID> routineIds = new ArrayList<>();
        String[] routineNames = {"Push", "Pull", "Legs"};
        for (int index = 0; index < routineNames.length; index++) {
            routineIds.add(createRoutine(userId, routineNames[index], exerciseIds.get(index)));
        }
        seedSessions(userId, exerciseIds, routineIds);
    }

    private List<UUID> exerciseIds() {
        List<UUID> ids = new ArrayList<>();
        for (String name : EXERCISES) {
            List<UUID> found = jdbc.query(
                    "SELECT id FROM exercises WHERE name_es = ?", (rs, rowNum) -> rs.getObject("id", UUID.class), name);
            if (!found.isEmpty()) {
                ids.add(found.getFirst());
            }
        }
        return ids;
    }

    private UUID createRoutine(UUID userId, String name, UUID exerciseId) {
        UUID routineId = UuidV7.generate();
        jdbc.update(
                "INSERT INTO routines (id, user_id, name, notes) VALUES (?, ?, ?, ?)",
                routineId,
                userId,
                "Demo " + name,
                "Rutina de ejemplo para probar Ferro");
        UUID entryId = UuidV7.generate();
        jdbc.update("""
                INSERT INTO routine_exercises (id, routine_id, exercise_id, position, rest_seconds)
                VALUES (?, ?, ?, 0, 120)
                """, entryId, routineId, exerciseId);
        for (int set = 0; set < 3; set++) {
            jdbc.update("""
                    INSERT INTO routine_sets
                        (id, routine_exercise_id, position, set_type, target_reps, target_weight, target_rir)
                    VALUES (?, ?, ?, 'NORMAL', 8, ?, 2)
                    """, UuidV7.generate(), entryId, set, BigDecimal.valueOf(60));
        }
        return routineId;
    }

    private void seedSessions(UUID userId, List<UUID> exerciseIds, List<UUID> routineIds) {
        ZoneId zone = ZoneId.of("Europe/Madrid");
        LocalDate monday = LocalDate.now(zone)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(11);
        for (int week = 0; week < 12; week++) {
            for (int day : new int[] {0, 2, 4}) {
                LocalDate date = monday.plusWeeks(week).plusDays(day);
                Instant started = date.atTime(18, 0).atZone(zone).toInstant();
                Instant finished = started.plusSeconds(3600 + week * 20L + day * 10L);
                UUID sessionId = UuidV7.generate();
                UUID routineId = routineIds.get(week % routineIds.size());
                BigDecimal totalVolume = BigDecimal.ZERO;
                jdbc.update(
                        """
                        INSERT INTO workout_sessions
                            (id, user_id, routine_id, name, status, started_at, finished_at,
                             duration_seconds, total_volume, notes)
                        VALUES (?, ?, ?, ?, 'FINISHED', ?, ?, ?, 0, 'Sesión demo')
                        """,
                        sessionId,
                        userId,
                        routineId,
                        "Demo " + date,
                        Timestamp.from(started),
                        Timestamp.from(finished),
                        3720 + week * 20 + day * 10);
                for (int exerciseIndex = 0; exerciseIndex < Math.min(2, exerciseIds.size()); exerciseIndex++) {
                    UUID entryId = UuidV7.generate();
                    jdbc.update("""
                            INSERT INTO session_exercises
                                (id, session_id, exercise_id, position, rest_seconds, superset_group)
                            VALUES (?, ?, ?, ?, 120, NULL)
                            """, entryId, sessionId, exerciseIds.get(exerciseIndex), exerciseIndex);
                    for (int set = 0; set < 3; set++) {
                        int reps = 8;
                        BigDecimal weight = BigDecimal.valueOf(50 + week * 2L + exerciseIndex * 20L + set * 2L);
                        BigDecimal volume = weight.multiply(BigDecimal.valueOf(reps));
                        totalVolume = totalVolume.add(volume);
                        UUID setId = UuidV7.generate();
                        jdbc.update("""
                                INSERT INTO session_sets
                                    (id, session_exercise_id, position, set_type, reps, weight, rir, logged_at, notes)
                                VALUES (?, ?, ?, 'NORMAL', ?, ?, 2, ?, NULL)
                                """, setId, entryId, set, reps, weight, Timestamp.from(finished));
                    }
                }
                jdbc.update("UPDATE workout_sessions SET total_volume = ? WHERE id = ?", totalVolume, sessionId);
            }
        }
        seedRecords(userId, exerciseIds.getFirst());
    }

    private void seedRecords(UUID userId, UUID exerciseId) {
        List<UUID> sets = jdbc.query("""
                SELECT ss.id, ss.weight, ss.logged_at FROM session_sets ss
                JOIN session_exercises se ON se.id = ss.session_exercise_id
                JOIN workout_sessions ws ON ws.id = se.session_id
                WHERE ws.user_id = ? AND se.exercise_id = ?
                ORDER BY ss.logged_at DESC LIMIT 1
                """, (rs, rowNum) -> rs.getObject("id", UUID.class), userId, exerciseId);
        if (sets.isEmpty()) {
            return;
        }
        UUID setId = sets.getFirst();
        Instant achieved = jdbc.queryForObject(
                        "SELECT logged_at FROM session_sets WHERE id = ?", Timestamp.class, setId)
                .toInstant();
        BigDecimal value = jdbc.queryForObject("SELECT weight FROM session_sets WHERE id = ?", BigDecimal.class, setId);
        namedJdbc.update(
                """
                INSERT INTO personal_records (id, user_id, exercise_id, pr_type, value, achieved_at, session_set_id)
                VALUES (:id, :userId, :exerciseId, 'MAX_WEIGHT', :value, :achieved, :setId),
                       (:id2, :userId, :exerciseId, 'MAX_E1RM', :e1rm, :achieved, :setId)
                """,
                new MapSqlParameterSource()
                        .addValue("id", UuidV7.generate())
                        .addValue("id2", UuidV7.generate())
                        .addValue("userId", userId)
                        .addValue("exerciseId", exerciseId)
                        .addValue("value", value)
                        .addValue("e1rm", value.multiply(BigDecimal.valueOf(1 + 8 / 30.0)))
                        .addValue("achieved", Timestamp.from(achieved))
                        .addValue("setId", setId));
    }
}
