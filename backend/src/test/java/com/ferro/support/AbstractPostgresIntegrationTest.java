package com.ferro.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL 18 real para todos los tests de integración.
 *
 * <p>Un único contenedor por JVM (patrón singleton de Testcontainers): rápido y suficiente
 * para el tamaño del proyecto. Nunca H2 (ADR-0007).
 */
public abstract class AbstractPostgresIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:18-alpine"))
            .withDatabaseName("ferro_test")
            .withUsername("ferro")
            // Contenedor efímero por JVM: la password no protege nada fuera del test.
            .withPassword("ferro"); // ggignore

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
