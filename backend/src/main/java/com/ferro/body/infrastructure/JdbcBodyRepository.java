package com.ferro.body.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ferro.body.application.port.BodyRepository;
import com.ferro.body.domain.BodyMeasurement;
import com.ferro.body.domain.BodyPage;

@Repository
public class JdbcBodyRepository implements BodyRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcBodyRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public BodyPage findAll(UUID userId, Instant from, Instant to, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("from", from == null ? null : Timestamp.from(from))
                .addValue("to", to == null ? null : Timestamp.from(to))
                .addValue("limit", size)
                .addValue("offset", page * size);
        Long total = jdbc.queryForObject("""
                SELECT count(*) FROM body_measurements
                WHERE user_id = :userId
                  AND measured_at >= coalesce(:from, '-infinity'::timestamptz)
                  AND measured_at < coalesce(:to, 'infinity'::timestamptz)
                """, params, Long.class);
        List<BodyMeasurement> content = jdbc.query("""
                SELECT id, measured_at, weight, chest, waist, hip, arm, thigh, notes
                FROM body_measurements
                WHERE user_id = :userId
                  AND measured_at >= coalesce(:from, '-infinity'::timestamptz)
                  AND measured_at < coalesce(:to, 'infinity'::timestamptz)
                ORDER BY measured_at DESC
                LIMIT :limit OFFSET :offset
                """, params, (rs, rowNum) -> map(rs));
        long totalElements = total == null ? 0 : total;
        return new BodyPage(content, page, size, totalElements, (int) Math.ceil((double) totalElements / size));
    }

    @Override
    public Optional<BodyMeasurement> find(UUID userId, UUID id) {
        return jdbc
                .query(
                        """
                SELECT id, measured_at, weight, chest, waist, hip, arm, thigh, notes
                FROM body_measurements WHERE id = :id AND user_id = :userId
                """,
                        new MapSqlParameterSource().addValue("id", id).addValue("userId", userId),
                        (rs, rowNum) -> map(rs))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<BodyMeasurement> findByDay(UUID userId, Instant measuredAt, String zone) {
        return jdbc
                .query(
                        """
                SELECT id, measured_at, weight, chest, waist, hip, arm, thigh, notes
                FROM body_measurements
                WHERE user_id = :userId
                  AND (measured_at AT TIME ZONE :zone)::date = (:measuredAt AT TIME ZONE :zone)::date
                ORDER BY measured_at DESC LIMIT 1
                """,
                        new MapSqlParameterSource()
                                .addValue("userId", userId)
                                .addValue("zone", zone)
                                .addValue("measuredAt", Timestamp.from(measuredAt)),
                        (rs, rowNum) -> map(rs))
                .stream()
                .findFirst();
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
    public void create(UUID userId, BodyMeasurement measurement) {
        jdbc.update("""
                INSERT INTO body_measurements
                    (id, user_id, measured_at, weight, chest, waist, hip, arm, thigh, notes)
                VALUES (:id, :userId, :measuredAt, :weight, :chest, :waist, :hip, :arm, :thigh, :notes)
                """, params(userId, measurement));
    }

    @Override
    public void update(UUID userId, BodyMeasurement measurement) {
        jdbc.update("""
                UPDATE body_measurements SET measured_at = :measuredAt, weight = :weight,
                    chest = :chest, waist = :waist, hip = :hip, arm = :arm, thigh = :thigh,
                    notes = :notes
                WHERE id = :id AND user_id = :userId
                """, params(userId, measurement));
    }

    @Override
    public void delete(UUID userId, UUID id) {
        jdbc.update(
                "DELETE FROM body_measurements WHERE id = :id AND user_id = :userId",
                new MapSqlParameterSource().addValue("id", id).addValue("userId", userId));
    }

    @Override
    public List<BodyMeasurement> since(UUID userId, Instant from) {
        return jdbc.query(
                """
                SELECT id, measured_at, weight, chest, waist, hip, arm, thigh, notes
                FROM body_measurements WHERE user_id = :userId AND measured_at >= :from
                ORDER BY measured_at
                """,
                new MapSqlParameterSource().addValue("userId", userId).addValue("from", Timestamp.from(from)),
                (rs, rowNum) -> map(rs));
    }

    private MapSqlParameterSource params(UUID userId, BodyMeasurement measurement) {
        return new MapSqlParameterSource()
                .addValue("id", measurement.id())
                .addValue("userId", userId)
                .addValue("measuredAt", Timestamp.from(measurement.measuredAt()))
                .addValue("weight", measurement.weight())
                .addValue("chest", measurement.chest())
                .addValue("waist", measurement.waist())
                .addValue("hip", measurement.hip())
                .addValue("arm", measurement.arm())
                .addValue("thigh", measurement.thigh())
                .addValue("notes", measurement.notes());
    }

    private BodyMeasurement map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new BodyMeasurement(
                rs.getObject("id", UUID.class),
                rs.getTimestamp("measured_at").toInstant(),
                rs.getBigDecimal("weight"),
                rs.getBigDecimal("chest"),
                rs.getBigDecimal("waist"),
                rs.getBigDecimal("hip"),
                rs.getBigDecimal("arm"),
                rs.getBigDecimal("thigh"),
                rs.getString("notes"));
    }
}
