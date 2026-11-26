package com.ferro.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

import com.ferro.support.AbstractPostgresIntegrationTest;

/**
 * Gate de M0: todas las migraciones se aplican sobre una base de datos vacía y el esquema
 * resultante es usable. Se ejecuta en una BD nueva para no depender del orden de otros tests.
 */
class FlywayMigrationsTest extends AbstractPostgresIntegrationTest {

    private static final String MIGRATION_DB = "ferro_migrations_test";

    @Test
    void migrations_apply_cleanly_on_an_empty_database() throws Exception {
        createEmptyDatabase();

        String url = POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + MIGRATION_DB);
        Flyway flyway = Flyway.configure()
                .dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied())
                .extracting(info -> info.getVersion().getVersion())
                .contains("1", "2", "3", "4", "5");

        try (Connection connection = DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword())) {
            assertThat(extensionCount(connection)).isEqualTo(2);
            assertThat(enumCount(connection)).isEqualTo(10);
            assertThat(tableCount(connection)).isEqualTo(15);
            assertThat(flagCount(connection)).isEqualTo(7);
            assertThat(activeSessionIndexExists(connection)).isTrue();

            UUID userId = insertUser(connection, "Test@Ferro.dev");
            // citext hace comparación case-insensitive, pero conserva el texto original:
            // la normalización a minúsculas es responsabilidad del VO Email (docs/05 §6.5).
            assertThat(storedEmail(connection, "test@ferro.dev")).isEqualTo("Test@Ferro.dev");
            assertThatThrownBy(() -> insertUser(connection, "test@ferro.dev"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("duplicate key");

            // RN-08: la BD impide dos sesiones ACTIVE del mismo usuario.
            insertActiveSession(connection, userId, "Sesión A");
            assertThatThrownBy(() -> insertActiveSession(connection, userId, "Sesión B"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("duplicate key");

            // RN-23: una medición sin ningún campo medible no es válida.
            assertThatThrownBy(() -> insertEmptyMeasurement(connection, userId))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("check constraint");
        }
    }

    private void createEmptyDatabase() throws Exception {
        POSTGRES.execInContainer(
                "psql",
                "-U",
                POSTGRES.getUsername(),
                "-d",
                "postgres",
                "-c",
                "DROP DATABASE IF EXISTS " + MIGRATION_DB + " WITH (FORCE)");
        POSTGRES.execInContainer(
                "psql", "-U", POSTGRES.getUsername(), "-d", "postgres", "-c", "CREATE DATABASE " + MIGRATION_DB);
    }

    private int extensionCount(Connection connection) throws SQLException {
        return scalar(connection, "select count(*) from pg_extension where extname in ('citext', 'pg_trgm')");
    }

    private int enumCount(Connection connection) throws SQLException {
        return scalar(connection, """
                select count(*) from pg_type t
                join pg_namespace n on n.oid = t.typnamespace
                where n.nspname = 'public' and t.typtype = 'e'
                """);
    }

    private int tableCount(Connection connection) throws SQLException {
        return scalar(connection, """
                select count(*) from information_schema.tables
                where table_schema = 'public'
                  and table_name in (
                    'users', 'oauth_accounts', 'refresh_tokens',
                    'email_verification_tokens', 'password_reset_tokens',
                    'exercises',
                    'routines', 'routine_exercises', 'routine_sets',
                    'workout_sessions', 'session_exercises', 'session_sets',
                    'personal_records', 'body_measurements',
                    'feature_flags'
                  )
                """);
    }

    private int flagCount(Connection connection) throws SQLException {
        return scalar(connection, "select count(*) from feature_flags");
    }

    private boolean activeSessionIndexExists(Connection connection) throws SQLException {
        return scalar(
                        connection,
                        "select count(*) from pg_indexes where schemaname = 'public' "
                                + "and indexname = 'uq_workout_sessions_active'")
                == 1;
    }

    private int scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private UUID insertUser(Connection connection, String email) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result =
                        statement.executeQuery("insert into users (email, display_name, health_consent_at) values ('"
                                + email + "', 'Test', now()) returning id")) {
            assertThat(result.next()).isTrue();
            return UUID.fromString(result.getString(1));
        }
    }

    private void insertActiveSession(Connection connection, UUID userId, String name) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("insert into workout_sessions (user_id, name, status, started_at) values ('" + userId
                    + "', '" + name + "', 'ACTIVE', now())");
        }
    }

    private void insertEmptyMeasurement(Connection connection, UUID userId) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("insert into body_measurements (user_id, measured_at) values ('" + userId + "', now())");
        }
    }

    private String storedEmail(Connection connection, String email) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result =
                        statement.executeQuery("select email::text from users where email = '" + email + "'")) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }
}
