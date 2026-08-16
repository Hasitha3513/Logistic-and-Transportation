package com.transportlogistics.app.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("postgres")
@Testcontainers
public abstract class PostgreSqlIntegrationTest {
    @Container
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.4-alpine")
            .withDatabaseName("transport_integration")
            .withUsername("transport_test")
            .withPassword("transport_test");

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("security.jwt.secret", () -> "postgresql-integration-test-secret-32-bytes-minimum");
        registry.add("app.dev.identity-bootstrap.enabled", () -> "false");
        registry.add("app.dev.sample-data.enabled", () -> "false");
    }
}
