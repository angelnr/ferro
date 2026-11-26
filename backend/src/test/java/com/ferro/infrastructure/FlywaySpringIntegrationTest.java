package com.ferro.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ferro.support.AbstractPostgresIntegrationTest;

/**
 * Gate de arranque: Spring Boot debe ejecutar Flyway antes de crear el EntityManagerFactory.
 *
 * <p>Este test habría detectado que Boot 4 modularizó la autoconfiguración de Flyway y que
 * {@code flyway-core} a secas no basta.
 */
@SpringBootTest
class FlywaySpringIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void spring_boot_applies_pending_migrations_on_startup() {
        assertThat(flyway.info().applied())
                .extracting(info -> info.getVersion().getVersion())
                .contains("1");
        assertThat(flyway.info().pending()).isEmpty();
    }
}
