package com.ferro.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
                .contains("1");

        try (Connection connection = DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword())) {
            assertThat(extensionCount(connection)).isEqualTo(2);
            assertThat(tableCount(connection)).isEqualTo(5);

            insertUser(connection, "Test@Ferro.dev");
            // citext hace comparación case-insensitive, pero conserva el texto original:
            // la normalización a minúsculas es responsabilidad del VO Email (docs/05 §6.5).
            assertThat(storedEmail(connection, "test@ferro.dev")).isEqualTo("Test@Ferro.dev");
            assertThatThrownBy(() -> insertUser(connection, "test@ferro.dev"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("duplicate key");
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

    private int tableCount(Connection connection) throws SQLException {
        return scalar(connection, """
                select count(*) from information_schema.tables
                where table_schema = 'public'
                  and table_name in ('users', 'oauth_accounts', 'refresh_tokens',
                                     'email_verification_tokens', 'password_reset_tokens')
                """);
    }

    private int scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private void insertUser(Connection connection, String email) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("insert into users (email, display_name, health_consent_at) values ('" + email
                    + "', 'Test', now())");
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
