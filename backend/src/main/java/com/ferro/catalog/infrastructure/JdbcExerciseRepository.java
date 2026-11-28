package com.ferro.catalog.infrastructure;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ferro.catalog.application.port.ExerciseRepository;
import com.ferro.catalog.domain.Equipment;
import com.ferro.catalog.domain.Exercise;
import com.ferro.catalog.domain.Mechanics;
import com.ferro.catalog.domain.MuscleGroup;
import com.ferro.catalog.domain.PageResult;

@Repository
public class JdbcExerciseRepository implements ExerciseRepository {

    private static final RowMapper<Exercise> ROW_MAPPER = JdbcExerciseRepository::map;
    private static final Set<String> STOPWORDS =
            Set.of("de", "del", "con", "en", "la", "el", "los", "las", "and", "with", "the", "of");

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcExerciseRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PageResult<Exercise> search(
            UUID userId, String query, MuscleGroup muscleGroup, Equipment equipment, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        StringBuilder where = new StringBuilder(" WHERE NOT archived AND (NOT is_custom OR owner_id = :userId)");
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        if (query != null && !query.isBlank()) {
            params.addValue("q", query.trim());
            String[] rawTokens = query.trim().split("\\s+");
            String[] filtered = Arrays.stream(rawTokens)
                    .filter(token -> !STOPWORDS.contains(token.toLowerCase(Locale.ROOT)))
                    .toArray(String[]::new);
            String[] tokens = filtered.length == 0 ? rawTokens : filtered;
            for (int i = 0; i < tokens.length; i++) {
                String name = "q" + i;
                where.append(" AND (unaccent(lower(name_es)) LIKE '%' || unaccent(lower(:")
                        .append(name)
                        .append(")) || '%' OR unaccent(lower(name_en)) LIKE '%' || unaccent(lower(:")
                        .append(name)
                        .append(")) || '%')");
                params.addValue(name, tokens[i]);
            }
        }
        if (muscleGroup != null) {
            where.append(" AND muscle_group = CAST(:muscle AS muscle_group)");
            params.addValue("muscle", muscleGroup.name());
        }
        if (equipment != null) {
            where.append(" AND equipment = CAST(:equipment AS equipment)");
            params.addValue("equipment", equipment.name());
        }

        String countSql = "SELECT count(*) FROM exercises" + where;
        Long total = jdbc.queryForObject(countSql, params, Long.class);

        String order = query != null && !query.isBlank()
                ? " ORDER BY similarity(unaccent(lower(name_es)), unaccent(lower(:q))) DESC, name_es ASC"
                : " ORDER BY name_es ASC";
        String sql = "SELECT id, name_es, name_en, muscle_group, secondary_muscles, equipment, mechanics,"
                + " bodyweight, is_custom, owner_id, archived FROM exercises"
                + where
                + order
                + " LIMIT :limit OFFSET :offset";
        params.addValue("limit", safeSize);
        params.addValue("offset", safePage * safeSize);

        List<Exercise> content = jdbc.query(sql, params, ROW_MAPPER);
        return new PageResult<>(content, safePage, safeSize, total == null ? 0 : total);
    }

    private static Exercise map(ResultSet rs, int rowNum) throws SQLException {
        Array secondary = rs.getArray("secondary_muscles");
        Object[] raw = secondary == null ? new Object[0] : (Object[]) secondary.getArray();
        List<MuscleGroup> secondaryMuscles = new ArrayList<>(raw.length);
        for (Object value : raw) {
            secondaryMuscles.add(MuscleGroup.valueOf(String.valueOf(value)));
        }
        return new Exercise(
                rs.getObject("id", UUID.class),
                rs.getString("name_es"),
                rs.getString("name_en"),
                MuscleGroup.valueOf(rs.getString("muscle_group")),
                List.copyOf(secondaryMuscles),
                Equipment.valueOf(rs.getString("equipment")),
                Mechanics.valueOf(rs.getString("mechanics")),
                rs.getBoolean("bodyweight"),
                rs.getBoolean("is_custom"),
                rs.getObject("owner_id", UUID.class),
                rs.getBoolean("archived"));
    }

    @Override
    public Optional<Exercise> findById(UUID id) {
        List<Exercise> found = jdbc.query(
                "SELECT id, name_es, name_en, muscle_group, secondary_muscles, equipment, mechanics,"
                        + " bodyweight, is_custom, owner_id, archived FROM exercises WHERE id = :id",
                new MapSqlParameterSource("id", id),
                ROW_MAPPER);
        return found.stream().findFirst();
    }

    @Override
    public Exercise save(Exercise exercise) {
        jdbc.getJdbcTemplate().update("""
                INSERT INTO exercises (id, name_es, name_en, muscle_group, secondary_muscles, equipment,
                    mechanics, bodyweight, is_custom, owner_id, archived)
                VALUES (?, ?, ?, CAST(? AS muscle_group),
                    CAST(? AS muscle_group[]), CAST(? AS equipment), CAST(? AS mechanics), ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    name_es = excluded.name_es,
                    name_en = excluded.name_en,
                    muscle_group = excluded.muscle_group,
                    secondary_muscles = excluded.secondary_muscles,
                    equipment = excluded.equipment,
                    mechanics = excluded.mechanics,
                    bodyweight = excluded.bodyweight,
                    archived = excluded.archived,
                    updated_at = now()
                """, ps -> {
            ps.setObject(1, exercise.id());
            ps.setString(2, exercise.nameEs());
            ps.setString(3, exercise.nameEn());
            ps.setString(4, exercise.muscleGroup().name());
            Array secondary = ps.getConnection()
                    .createArrayOf(
                            "muscle_group",
                            exercise.secondaryMuscles().stream().map(Enum::name).toArray(String[]::new));
            ps.setArray(5, secondary);
            ps.setString(6, exercise.equipment().name());
            ps.setString(7, exercise.mechanics().name());
            ps.setBoolean(8, exercise.bodyweight());
            ps.setBoolean(9, exercise.custom());
            ps.setObject(10, exercise.ownerId());
            ps.setBoolean(11, exercise.archived());
        });
        return exercise;
    }

    @Override
    public long countCustomByOwner(UUID ownerId) {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM exercises WHERE is_custom AND owner_id = :ownerId",
                new MapSqlParameterSource("ownerId", ownerId),
                Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public boolean hasHistory(UUID exerciseId) {
        Long count = jdbc.queryForObject("""
                SELECT count(*) FROM session_exercises se
                JOIN session_sets ss ON ss.session_exercise_id = se.id
                WHERE se.exercise_id = :exerciseId
                """, new MapSqlParameterSource("exerciseId", exerciseId), Long.class);
        return count != null && count > 0;
    }
}
