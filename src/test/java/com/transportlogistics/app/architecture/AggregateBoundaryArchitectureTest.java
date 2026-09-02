package com.transportlogistics.app.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AggregateBoundaryArchitectureTest {

    private static final Path JAVA_ROOT = Path.of("src/main/java/com/transportlogistics/app");

    private static final Set<String> APPROVED_ASSOCIATION_OWNERS = Set.of(
            "delivery/adapters/outbound/persistence/DeliveryExceptionCaseEntity.java",
            "delivery/adapters/outbound/persistence/DeliveryExceptionEvidenceEntity.java",
            "freight/exception/adapters/outbound/persistence/CargoExceptionEntity.java",
            "freight/exception/adapters/outbound/persistence/CargoExceptionHistoryEntity.java",
            "freight/insurance/adapters/outbound/persistence/FreightInsuranceClaimEntity.java",
            "freight/insurance/adapters/outbound/persistence/FreightInsuranceSettlementEntity.java",
            "freight/loadplanning/adapters/outbound/persistence/LoadPlanEntity.java",
            "freight/loadplanning/adapters/outbound/persistence/LoadPlanItemPlacementEntity.java",
            "freight/manifest/adapters/outbound/persistence/CargoManifestEntity.java",
            "freight/manifest/adapters/outbound/persistence/CargoManifestItemEntity.java",
            "freight/order/adapters/outbound/persistence/FreightOrderEntity.java",
            "freight/order/adapters/outbound/persistence/FreightOrderLineEntity.java"
    );

    private static final Set<String> APPROVED_CASCADE_ROOTS = Set.of(
            "delivery/adapters/outbound/persistence/DeliveryExceptionCaseEntity.java",
            "freight/exception/adapters/outbound/persistence/CargoExceptionEntity.java",
            "freight/insurance/adapters/outbound/persistence/FreightInsuranceClaimEntity.java",
            "freight/loadplanning/adapters/outbound/persistence/LoadPlanEntity.java",
            "freight/manifest/adapters/outbound/persistence/CargoManifestEntity.java",
            "freight/order/adapters/outbound/persistence/FreightOrderEntity.java"
    );

    @Test
    void jpaObjectGraphsMustRemainInsideExplicitAggregateOwnership() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path source : javaSources()) {
            String text = Files.readString(source);
            String relative = relative(source);
            if (declaresJpaAssociation(text) && !APPROVED_ASSOCIATION_OWNERS.contains(relative)) {
                violations.add(relative + " declares an unapproved JPA entity association");
            }
            if (text.contains("cascade =") && !APPROVED_CASCADE_ROOTS.contains(relative)) {
                violations.add(relative + " declares cascade behavior outside an approved aggregate root");
            }
        }

        assertThat(violations)
                .as("cross-aggregate references must remain IDs; JPA graphs and cascades require explicit ownership")
                .isEmpty();
    }

    @Test
    void approvedAssociationBaselineMustMatchProductionSources() throws IOException {
        var detected = javaSources().stream().filter(path -> {
            try {
                return declaresJpaAssociation(Files.readString(path));
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }).map(AggregateBoundaryArchitectureTest::relative).collect(java.util.stream.Collectors.toSet());

        assertThat(detected).containsExactlyInAnyOrderElementsOf(APPROVED_ASSOCIATION_OWNERS);
    }

    private static boolean declaresJpaAssociation(String source) {
        return source.contains("@OneToMany") || source.contains("@ManyToOne")
                || source.contains("@OneToOne") || source.contains("@ManyToMany");
    }

    private static List<Path> javaSources() throws IOException {
        try (var sources = Files.walk(JAVA_ROOT)) {
            return sources.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private static String relative(Path source) {
        return JAVA_ROOT.relativize(source).toString().replace('\\', '/');
    }
}
