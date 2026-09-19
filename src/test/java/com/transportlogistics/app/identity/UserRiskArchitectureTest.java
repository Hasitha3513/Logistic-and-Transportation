package com.transportlogistics.app.identity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class UserRiskArchitectureTest {

    @Test
    void riskDomainAndPortRemainFrameworkAndPersistenceNeutral() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.transportlogistics.app.identity");

        noClasses().that().resideInAnyPackage(
                        "..identity.domain.risk..", "..identity.application.ports.out..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "org.hibernate..",
                        "com.fasterxml.jackson..",
                        "..identity.infrastructure..")
                .check(classes);
    }

    @Test
    void firstWaveRiskContractsDoNotDependOnForeignBusinessModules() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.transportlogistics.app.identity.domain.risk");

        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                        "..operations..", "..tracking..", "..delivery..", "..fuel..", "..fleet..")
                .check(classes);
    }
}
