package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryNumberPostgreSqlAcceptanceTest {
    private static JdbcTemplate jdbc;
    private static PostgresDeliveryNumberGenerator numbers;

    @BeforeAll
    static void migrate() {
        String url = System.getProperty("DB_URL", "jdbc:postgresql://localhost:5432/delivery_acceptance");
        String username = System.getProperty("DB_USERNAME", "transport_app");
        String password = System.getProperty("DB_PASSWORD", "transport_app_secret");
        var dataSource = new DriverManagerDataSource(url, username, password);
        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        numbers = new PostgresDeliveryNumberGenerator(jdbc);
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void allocatesUniqueNumbersUnderSameTenantYearConcurrency() throws Exception {
        UUID tenant = UUID.randomUUID();
        var pool = Executors.newFixedThreadPool(10);
        try {
            List<Callable<String>> calls = java.util.stream.IntStream.range(0, 20)
                    .mapToObj(index -> (Callable<String>) () -> numbers.next(tenant, Year.of(2026)).value()).toList();
            var allocated = pool.invokeAll(calls).stream().map(future -> {
                try { return future.get(); } catch (Exception error) { throw new AssertionError(error); }
            }).toList();
            assertThat(new HashSet<>(allocated)).hasSize(20);
            assertThat(allocated).allMatch(value -> value.matches("DEL-2026-[0-9]{6}"));
            assertThat(allocated.stream().sorted().toList()).containsExactlyElementsOf(
                    java.util.stream.IntStream.rangeClosed(1, 20).mapToObj(value -> "DEL-2026-%06d".formatted(value)).toList());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void isolatesCountersByTenantAndCalendarYear() {
        UUID tenantA = UUID.randomUUID(), tenantB = UUID.randomUUID();
        assertThat(numbers.next(tenantA, Year.of(2026)).value()).isEqualTo("DEL-2026-000001");
        assertThat(numbers.next(tenantB, Year.of(2026)).value()).isEqualTo("DEL-2026-000001");
        assertThat(numbers.next(tenantA, Year.of(2027)).value()).isEqualTo("DEL-2027-000001");
        assertThat(numbers.next(tenantA, Year.of(2026)).value()).isEqualTo("DEL-2026-000002");
    }

    @Test
    void migratesV46WithUs56SchemaConstraintsIndexesAndPermissions() {
        assertThat(jdbc.queryForObject("SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1", String.class)).isEqualTo("46");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('delivery_order','delivery_number_counter')", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pg_indexes WHERE tablename='delivery_order' AND indexname IN ('idx_delivery_order_tenant_status','idx_delivery_order_tenant_customer','idx_delivery_order_tenant_window','uk_delivery_order_tenant_number')", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForList("SELECT code FROM app_permission WHERE code LIKE 'DELIVERY_%' ORDER BY code", String.class))
                .containsExactly("DELIVERY_ASSIGN", "DELIVERY_CREATE", "DELIVERY_UPDATE", "DELIVERY_VIEW");
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_name='delivery_order' AND column_name='tenant_id'", String.class)).isEqualTo("NO");
    }
}
