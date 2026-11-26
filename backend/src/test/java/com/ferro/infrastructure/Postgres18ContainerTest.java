package com.ferro.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

import com.ferro.support.AbstractPostgresIntegrationTest;

/**
 * Humo del harness de integración: PostgreSQL real 18 en Testcontainers.
 *
 * <p>No usa H2 a propósito (ADR-0007): los índices parciales, pg_trgm y uuidv7 se comportan
 * distinto. Este test demuestra que Docker-in-Docker y el driver funcionan antes de escribir
 * los repositorios de M1.
 */
class Postgres18ContainerTest extends AbstractPostgresIntegrationTest {

    @Test
    void postgres_is_reachable_and_runs_major_version_18() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("select current_setting('server_version_num')")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1) / 10_000).isEqualTo(18);
        }
    }
}
