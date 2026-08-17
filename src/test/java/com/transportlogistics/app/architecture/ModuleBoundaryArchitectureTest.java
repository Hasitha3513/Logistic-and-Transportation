package com.transportlogistics.app.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ModuleBoundaryArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.transportlogistics.app");
    }

    @Test
    void tripModuleMustNotDirectlyAccessFleetRepositories() {
        noClasses()
                .that().resideInAPackage("..trip..")
                .should().dependOnClassesThat().resideInAPackage("..fleet.infrastructure.adapters.out.persistence..")
                .because("Trip module must not directly access Fleet persistence adapters or repositories")
                .check(importedClasses);
    }

    @Test
    void fuelModuleMustNotDirectlyAccessTripRepositories() {
        noClasses()
                .that().resideInAPackage("..fuel..")
                .should().dependOnClassesThat().resideInAnyPackage("..trip.infrastructure.adapters.out.persistence..")
                .because("Fuel module must not directly access Trip persistence adapters or repositories")
                .check(importedClasses);
    }

    @Test
    void identityModuleMustNotDirectlyAccessOrganizationRepositories() {
        noClasses()
                .that().resideInAPackage("..identity..")
                .should().dependOnClassesThat().resideInAnyPackage("..organization.infrastructure.adapters.out.persistence..")
                .because("Identity module must not directly access Organization persistence adapters or repositories")
                .check(importedClasses);
    }
}