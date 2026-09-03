package com.transportlogistics.app.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseTableOwnershipArchitectureTest {

    private static final Path JAVA_ROOT = Path.of("src/main/java/com/transportlogistics/app");
    private static final Path MIGRATION_ROOT = Path.of("src/main/resources/db/migration");
    private static final Pattern TABLE_MAPPING = Pattern.compile(
            "@(Table|CollectionTable)\\s*\\([^)]*?name\\s*=\\s*\"([^\"]+)\"", Pattern.DOTALL);
    private static final Pattern CREATED_TABLE = Pattern.compile(
            "(?i)create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?([a-z][a-z0-9_]*)");
    private static final Pattern JPA_REPOSITORY = Pattern.compile(
            "extends\\s+(?:TenantAware)?JpaRepository\\s*<\\s*([A-Za-z0-9_]+)");

    private static final Map<String, String> OWNERS = ownershipRegistry();

    private static final Set<String> APPROVED_FOREIGN_SQL_EXCEPTIONS = Set.of();

    @Test
    void everyFlywayTableHasExactlyOneDeclaredOwner() throws IOException {
        Set<String> createdTables = new HashSet<>();
        for (Path source : files(MIGRATION_ROOT, ".sql")) {
            var matcher = CREATED_TABLE.matcher(Files.readString(source));
            while (matcher.find()) {
                createdTables.add(matcher.group(1).toLowerCase());
            }
        }

        assertThat(OWNERS.keySet())
                .as("ownership registry must exactly cover tables created by Flyway")
                .containsExactlyInAnyOrderElementsOf(createdTables);
    }

    @Test
    void jpaMappedTablesAndRepositoriesMustStayInsideTheirOwnerModule() throws IOException {
        Map<String, String> entityModules = new HashMap<>();
        List<String> violations = new ArrayList<>();

        for (Path source : files(JAVA_ROOT, ".java")) {
            String text = Files.readString(source);
            String module = moduleOf(source);
            var tableMatcher = TABLE_MAPPING.matcher(text);
            while (tableMatcher.find()) {
                String table = tableMatcher.group(2);
                String owner = OWNERS.get(table);
                if (!module.equals(owner)) {
                    violations.add(relative(source) + " maps " + table + " owned by " + owner);
                }
            }
            if (text.contains("@Entity")) {
                String className = source.getFileName().toString().replace(".java", "");
                entityModules.put(className, module);
            }
        }

        for (Path source : files(JAVA_ROOT, ".java")) {
            String text = Files.readString(source);
            var repositoryMatcher = JPA_REPOSITORY.matcher(text);
            while (repositoryMatcher.find()) {
                String entity = repositoryMatcher.group(1);
                String entityModule = entityModules.get(entity);
                if (entityModule != null && !moduleOf(source).equals(entityModule)) {
                    violations.add(relative(source) + " is a repository for foreign entity " + entity);
                }
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void noNewProductionJdbcCodeMayAccessForeignTables() throws IOException {
        Set<String> detected = new HashSet<>();
        for (Path source : files(JAVA_ROOT, ".java")) {
            String text = Files.readString(source);
            if (!usesDirectDatabaseAccess(text)) {
                continue;
            }
            String module = moduleOf(source);
            for (var ownership : OWNERS.entrySet()) {
                if (!module.equals(ownership.getValue()) && containsSqlTableReference(text, ownership.getKey())) {
                    detected.add(relative(source) + "->" + ownership.getKey());
                }
            }
        }

        assertThat(detected)
                .as("legacy foreign-table SQL is not approval; P0-03 must remove this exact baseline")
                .containsExactlyInAnyOrderElementsOf(APPROVED_FOREIGN_SQL_EXCEPTIONS);
    }

    private static boolean usesDirectDatabaseAccess(String source) {
        return source.contains("JdbcTemplate") || source.contains("DataSource")
                || source.contains("EntityManager") || source.contains("nativeQuery = true");
    }

    private static boolean containsSqlTableReference(String source, String table) {
        return Pattern.compile("(?i)\\b(?:from|join|update|into|delete\\s+from)\\s+"
                        + Pattern.quote(table) + "(?![a-z0-9_])")
                .matcher(source).find();
    }

    private static List<Path> files(Path root, String suffix) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(suffix))
                    .sorted()
                    .toList();
        }
    }

    private static String moduleOf(Path source) {
        return JAVA_ROOT.relativize(source).getName(0).toString();
    }

    private static String relative(Path source) {
        return JAVA_ROOT.relativize(source).toString().replace('\\', '/');
    }

    private static Map<String, String> ownershipRegistry() {
        Map<String, String> ownership = new HashMap<>();
        own(ownership, "tenancy", "tenant");
        own(ownership, "identity", "app_user", "app_role", "app_permission", "app_user_role",
                "app_role_permission", "refresh_token", "tenant_membership", "tenant_membership_role");
        own(ownership, "organization", "customer", "department", "location", "project", "vendor");
        own(ownership, "fleet", "driver", "driver_license", "driver_exception", "driver_violation",
                "driver_medical_record", "driver_drug_test", "vehicle_category", "vehicle_type", "vehicle",
                "vehicle_document", "vehicle_reading", "vehicle_meter_reset", "maintenance_schedule", "lubricant_log");
        own(ownership, "routing", "route", "route_stop", "route_revision", "route_revision_stop", "route_disruption");
        own(ownership, "trip", "trip", "trip_status_history", "trip_dispatch", "trip_operational_event");
        own(ownership, "fuel", "fuel_station", "fuel_limit_policy", "fuel_issue", "fuel_issue_history",
                "fuel_price", "fuel_purchase", "fuel_purchase_history", "bunker_tank", "bunker_dip_reading",
                "bunker_stock_adjustment", "bunker_stock_movement");
        own(ownership, "freight", "freight_order", "freight_order_line", "cargo_manifest", "cargo_manifest_item",
                "load_plan", "load_plan_item_placement", "freight_insurance_policy", "freight_insurance_claim",
                "freight_insurance_settlement", "cargo_exception", "cargo_exception_history");
        own(ownership, "delivery", "delivery_order", "delivery_number_counter", "proof_of_delivery", "pod_evidence",
                "delivery_attempt", "delivery_contact_attempt", "delivery_escalation", "delivery_redelivery_schedule",
                "delivery_exception_case", "delivery_exception_evidence", "delivery_zone", "delivery_slot",
                "delivery_slot_reservation", "delivery_rider", "delivery_rider_zone", "delivery_rider_shift",
                "delivery_order_rider_assignment", "delivery_batch", "delivery_batch_order", "delivery_batch_counter",
                "delivery_self_service_access", "delivery_customer_submission");
        own(ownership, "notification", "notification", "notification_template", "notification_rule",
                "notification_rule_policy", "notification_rule_quiet_day", "notification_rule_execution",
                "notification_delivery_attempt", "customer_notification_preference");
        own(ownership, "offlinesync", "offline_sync_operation");
        return Map.copyOf(ownership);
    }

    private static void own(Map<String, String> ownership, String owner, String... tables) {
        for (String table : tables) {
            if (ownership.put(table, owner) != null) {
                throw new IllegalStateException("Duplicate table ownership declaration: " + table);
            }
        }
    }
}
