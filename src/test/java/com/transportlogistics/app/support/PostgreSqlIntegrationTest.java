package com.transportlogistics.app.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.AfterEach;
import org.flywaydb.core.Flyway;

import java.util.Locale;

@SpringBootTest
@ActiveProfiles("postgres")
public abstract class PostgreSqlIntegrationTest {

    private static final String MODE_ENV = "TRANSPORT_TEST_DB_MODE";
    private static final String LOCAL_MODE = "local";
    private static final TestDatabaseConfig DATABASE = TestDatabaseConfig.resolve();
    protected static final PostgreSQLContainer<?> POSTGRES;

    static {
        if (DATABASE.local()) {
            POSTGRES = null;
        } else {
            PostgreSQLContainer<?> container;
            try {
                if (!DockerClientFactory.instance().isDockerAvailable()) {
                    throw new IllegalStateException("Docker is required for PostgreSQL integration tests");
                }
                container = new PostgreSQLContainer<>("postgres:16-alpine")
                        .withDatabaseName("transport_integration")
                        .withUsername("transport_test")
                        .withPassword("transport_test");
                container.start();
            } catch (Throwable exception) {
                throw new IllegalStateException("PostgreSQL Testcontainer startup failed", exception);
            }
            POSTGRES = container;
        }
    }

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        if (DATABASE.local()) {
            registry.add("spring.datasource.url", DATABASE::jdbcUrl);
            registry.add("spring.datasource.username", DATABASE::username);
            registry.add("spring.datasource.password", DATABASE::password);
        } else if (POSTGRES != null && POSTGRES.isRunning()) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        } else {
            throw new IllegalStateException("PostgreSQL Testcontainer is unavailable");
        }
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.clean-disabled", () -> "false");
        registry.add("security.jwt.secret", () -> "postgresql-integration-test-secret-32-bytes-minimum");
        registry.add("app.dev.identity-bootstrap.enabled", () -> "false");
        registry.add("app.dev.sample-data.enabled", () -> "false");
    }
    @Autowired
    private Flyway flyway;

    @AfterEach
    void cleanDatabase() {
        // A plain TRUNCATE deletes migration-seeded templates and reference data while
        // leaving Flyway at V57, so later tests start from an invalid pseudo-baseline.
        // Rebuilding the isolated schema restores the exact production baseline.
        flyway.clean();
        flyway.migrate();
    }

    /**
     * Gives tests which need a direct JDBC connection the exact same explicitly
     * validated datasource selected for the Spring test context.
     */
    protected static String configuredJdbcUrl() {
        return DATABASE.local() ? DATABASE.jdbcUrl() : POSTGRES.getJdbcUrl();
    }

    protected static String configuredDatabaseUsername() {
        return DATABASE.local() ? DATABASE.username() : POSTGRES.getUsername();
    }

    protected static String configuredDatabasePassword() {
        return DATABASE.local() ? DATABASE.password() : POSTGRES.getPassword();
    }

    private record TestDatabaseConfig(boolean local, String jdbcUrl, String username, String password) {
        private static TestDatabaseConfig resolve() {
            var mode = System.getenv(MODE_ENV);
            if (mode == null || mode.isBlank() || "testcontainers".equalsIgnoreCase(mode)) {
                return new TestDatabaseConfig(false, null, null, null);
            }
            if (!LOCAL_MODE.equalsIgnoreCase(mode)) {
                throw new IllegalStateException(MODE_ENV + " must be 'testcontainers' or 'local'");
            }

            var url = required("TRANSPORT_TEST_DB_URL");
            var username = required("TRANSPORT_TEST_DB_USERNAME");
            var password = required("TRANSPORT_TEST_DB_PASSWORD");
            var normalizedUrl = url.toLowerCase(Locale.ROOT);
            var acknowledged = "true".equalsIgnoreCase(System.getenv("TRANSPORT_TEST_DB_ALLOW_DESTRUCTIVE"));
            if (!normalizedUrl.startsWith("jdbc:postgresql:")
                    || (!normalizedUrl.contains("_test") && !normalizedUrl.contains("_acceptance")
                    && !normalizedUrl.contains("_e2e") && !acknowledged)) {
                throw new IllegalStateException("Local PostgreSQL test database must use a test-only name "
                        + "(_test, _acceptance, or _e2e) or set TRANSPORT_TEST_DB_ALLOW_DESTRUCTIVE=true");
            }
            return new TestDatabaseConfig(true, url, username, password);
        }

        private static String required(String name) {
            var value = System.getenv(name);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(name + " is required when " + MODE_ENV + "=local");
            }
            return value;
        }
    }

}

