package com.transportlogistics.app.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
}
