package com.transportlogistics.app.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class HexagonalLayerArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.transportlogistics.app");
    }

    @Test
    void domainMustNotDependOnSpringWebOrMvc() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web..",
                        "org.springframework.http..",
                        "jakarta.servlet.."
                ).because("Domain layer must remain decoupled from HTTP/Web frameworks")
                .check(importedClasses);
    }

    @Test
    void domainMustNotDependOnJpaOrHibernate() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..",
                        "org.hibernate..",
                        "org.springframework.data.jpa..",
                        "org.springframework.data.repository.."
                ).because("Domain layer must not depend on JPA or persistence mechanisms")
                .check(importedClasses);
    }

    @Test
    void domainMustNotDependOnInfrastructureOrAdapters() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..infrastructure..",
                        "..adapters.."
                ).because("Domain layer must not depend on outer infrastructure/adapters")
                .check(importedClasses);
    }

    @Test
    void applicationMustNotDependOnInfrastructureAdapters() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..infrastructure.adapters..",
                        "..adapters.in.web..",
                        "..adapters.out.persistence.."
                ).because("Application services and ports must not depend on concrete adapter implementations")
                .check(importedClasses);
    }

    @Test
    void applicationMustNotDependOnSpringWeb() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web..",
                        "jakarta.servlet.."
                ).because("Application layer should not depend on HTTP/Web controllers or servlets")
                .check(importedClasses);
    }

    @Test
    void controllersMustResideInWebControllersSubpackage() {
        classes()
                .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().resideInAPackage("..web.controllers..")
                .because("REST controllers must strictly reside in web.controllers subpackage")
                .check(importedClasses);
    }

    @Test
    void webLayerClassesMustResideInDesignatedSubpackages() {
        classes()
                .that().resideInAPackage("..adapters.in.web..")
                .should().resideInAnyPackage(
                        "..web.controllers..",
                        "..web.dto..",
                        "..web.mappers.."
                )
                .because("Web layer classes must strictly reside in controllers, dto, or mappers subpackages")
                .check(importedClasses);
    }

    @Test
    void vehicleMasterDomainMustBeFrameworkFree() {
        classes()
                .that().resideInAPackage("..fleet.vehiclemaster.domain..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "java..",
                        "..fleet.vehiclemaster.domain.."
                ).because("Vehicle Master domain must use only Java and its own pure domain types")
                .check(importedClasses);
    }

    @Test
    void vehicleMasterPortsMustBeProviderNeutral() {
        noClasses()
                .that().resideInAPackage("..fleet.vehiclemaster.ports..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.hibernate..",
                        "com.fasterxml.jackson..",
                        "..fleet.vehiclemaster.adapters.."
                ).because("Vehicle Master ports must remain framework and provider neutral")
                .check(importedClasses);
    }

    @Test
    void vehicleMasterApplicationMustBeFrameworkAndAdapterFree() {
        noClasses()
                .that().resideInAPackage("..fleet.vehiclemaster.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.hibernate..",
                        "com.fasterxml.jackson..",
                        "..fleet.vehiclemaster.adapters.."
                ).because("Vehicle Master application services must depend inward through ports")
                .check(importedClasses);
    }

    @Test
    void vehicleMasterWebMustNotAccessPersistenceAdapters() {
        noClasses()
                .that().resideInAPackage("..fleet.vehiclemaster.adapters.inbound.web..")
                .should().dependOnClassesThat().resideInAPackage(
                        "..fleet.vehiclemaster.adapters.outbound.persistence.."
                ).because("Vehicle Master web adapters must invoke inbound ports")
                .check(importedClasses);
    }

    @Test
    void vehicleMasterPersistenceAdapterMustImplementRepositoryPort() {
        classes()
                .that().resideInAPackage("..fleet.vehiclemaster.adapters.outbound.persistence..")
                .and().haveSimpleNameEndingWith("PersistenceAdapter")
                .should().implement(VehicleRepository.class)
                .because("Vehicle Master persistence adapters implement outbound ports")
                .check(importedClasses);
    }

    @Test
    void otherModulesMustNotImportVehicleMasterAdapters() {
        noClasses()
                .that().resideOutsideOfPackage("..fleet..")
                .should().dependOnClassesThat().resideInAPackage("..fleet.vehiclemaster.adapters..")
                .because("Fleet adapters are internal implementation details")
                .check(importedClasses);
    }

    @Test
    void freightDomainPortsAndApplicationMustRemainFrameworkFree() {
        noClasses()
                .that().resideInAnyPackage("..freight..domain..", "..freight..ports..", "..freight..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "org.hibernate..", "com.fasterxml.jackson..",
                        "..freight..adapters..")
                .because("Freight core code must remain provider-neutral and depend inward")
                .check(importedClasses);
    }

    @Test
    void freightWebMustNotAccessPersistenceAdapters() {
        noClasses()
                .that().resideInAPackage("..freight..adapters.inbound.web..")
                .should().dependOnClassesThat().resideInAPackage("..freight..adapters.outbound.persistence..")
                .because("Freight web adapters must invoke inbound ports")
                .check(importedClasses);
    }
}
