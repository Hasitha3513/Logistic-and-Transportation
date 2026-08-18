package com.transportlogistics.app.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
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
}