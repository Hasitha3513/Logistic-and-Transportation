package com.transportlogistics.app.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.AfterEach;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@ActiveProfiles("postgres")
public abstract class PostgreSqlIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES;

    static {
        PostgreSQLContainer<?> container = null;
        try {
            if (DockerClientFactory.instance().isDockerAvailable()) {
                container = new PostgreSQLContainer<>("postgres:16.4-alpine")
                        .withDatabaseName("transport_integration")
                        .withUsername("transport_test")
                        .withPassword("transport_test");
                container.start();
            }
        } catch (Throwable ignored) {
            container = null;
        }
        POSTGRES = container;
    }

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        if (POSTGRES != null && POSTGRES.isRunning()) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> System.getProperty("DB_URL", "jdbc:postgresql://localhost:5432/transport_integration"));
            registry.add("spring.datasource.username", () -> System.getProperty("DB_USERNAME", "transport_app"));
            registry.add("spring.datasource.password", () -> System.getProperty("DB_PASSWORD", "LocalDb-Transport-2026"));
        }
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.clean-disabled", () -> "false");
        registry.add("security.jwt.secret", () -> "postgresql-integration-test-secret-32-bytes-minimum");
        registry.add("app.dev.identity-bootstrap.enabled", () -> "false");
        registry.add("app.dev.sample-data.enabled", () -> "false");
    }
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("DO $$ DECLARE r RECORD; BEGIN " +
                "FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename NOT IN ('flyway_schema_history')) LOOP " +
                "EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE;'; " +
                "END LOOP; END $$;");
    }

}

