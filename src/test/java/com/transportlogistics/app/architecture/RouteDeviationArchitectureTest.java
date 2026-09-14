package com.transportlogistics.app.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Arrays;

class RouteDeviationArchitectureTest {
    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.transportlogistics.app");
    }

    @Test
    void routeDeviationDomainAndPortsRemainFrameworkNeutral() {
        noClasses().that().resideInAnyPackage(
                        "..tracking.domain.routedeviation..",
                        "..tracking.ports.inbound..",
                        "..tracking.ports.outbound..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "org.hibernate..",
                        "com.fasterxml.jackson..", "..infrastructure..", "..adapters..")
                .check(productionClasses);
    }

    @Test
    void trackingDoesNotImportRoutingInternals() {
        noClasses().that().resideInAPackage("..tracking..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..routing.domain..", "..routing.application..", "..routing.infrastructure..")
                .check(productionClasses);
    }

    @Test
    void routingPublishedGeometryTypesAreNotPersistenceEntities() {
        noClasses().that().resideInAPackage("com.transportlogistics.app.routing")
                .should().beAnnotatedWith(Entity.class)
                .check(productionClasses);
    }

    @Test
    void publishedRouteDeviationEventsExposeOnlyPublishedPrimitiveTypes() {
        for (Class<?> type : java.util.List.of(
                com.transportlogistics.app.tracking.VehicleRouteDeviationDetectedV1.class,
                com.transportlogistics.app.tracking.VehicleRouteDeviationEscalatedV1.class)) {
            assertThat(Arrays.stream(type.getRecordComponents())).noneMatch(component ->
                    component.getType().getPackageName().contains("tracking.domain"));
        }
    }
}
