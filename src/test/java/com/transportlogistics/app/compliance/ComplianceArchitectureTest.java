package com.transportlogistics.app.compliance;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ComplianceArchitectureTest {

    @Test
    void domainAndPortsRemainFrameworkAndPersistenceNeutral() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.transportlogistics.app.compliance");

        noClasses().that().resideInAnyPackage(
                        "..compliance.domain..", "..compliance.ports..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "org.hibernate..",
                        "com.transportlogistics.app..adapters..",
                        "com.transportlogistics.app..infrastructure..")
                .check(classes);
    }

    @Test
    void complianceFoundationDoesNotDependOnForeignBusinessModules() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.transportlogistics.app.compliance");

        noClasses().that().resideInAPackage("..compliance..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..billing..", "..fleet..", "..freight..", "..tracking..", "..trip..")
                .check(classes);
    }
}
