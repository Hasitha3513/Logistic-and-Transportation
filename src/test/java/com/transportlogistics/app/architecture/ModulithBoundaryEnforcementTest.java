package com.transportlogistics.app.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.transportlogistics.app.TransportLogisticsApplication;
import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import org.springframework.modulith.core.ApplicationModules;

import java.lang.annotation.Annotation;
import java.util.Set;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModulithBoundaryEnforcementTest {

    private static final String BASE_PACKAGE = "com.transportlogistics.app";

    // P0-03 approved dependency graph. Cross-module targets are additionally constrained
    // below to published types in the provider module's root package.
    private static final Set<String> APPROVED_MODULE_DEPENDENCIES = Set.of(
            "delivery->fleet", "delivery->organization", "delivery->tenancy",
            "fleet->identity", "fleet->notification", "fleet->tenancy",
            "freight->fleet", "freight->organization", "freight->tenancy",
            "fuel->fleet", "fuel->identity", "fuel->organization", "fuel->trip",
            "identity->tenancy",
            "notification->identity", "notification->tenancy",
            "offlinesync->delivery", "offlinesync->fleet", "offlinesync->identity", "offlinesync->trip",
            "reporting->fleet", "reporting->freight", "reporting->trip",
            "routing->organization",
            "system->fleet", "system->organization", "system->trip",
            "trip->fleet", "trip->identity", "trip->notification", "trip->routing"
    );

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importProductionClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }

    @Test
    void springModulithStructureMustBeValid() {
        ApplicationModules.of(TransportLogisticsApplication.class).verify();
    }

    @Test
    void modulesMustBeFreeOfCycles() {
        slices().matching(BASE_PACKAGE + ".(*)..")
                .should().beFreeOfCycles()
                .check(importedClasses);
    }

    @Test
    void modulesMustNotAccessForeignRepositories() {
        classes().should(notDependOnForeignTypeAssignableTo(Repository.class, "repository"))
                .check(importedClasses);
    }

    @Test
    void jpaEntitiesMustNotReferenceForeignJpaEntities() {
        classes().that().areAnnotatedWith(Entity.class)
                .should(notDependOnForeignTypeAnnotatedWith(Entity.class, "JPA entity"))
                .check(importedClasses);
    }

    @Test
    void domainMustNotDependOnAdapterImplementationsOrPlatformModules() {
        noClasses().that().resideInAPackage("..domain..")
                .and().resideOutsideOfPackages(
                        "..reporting..", "..system..", "..tenancy..", "..identity..", "..shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adapters..", "..infrastructure..",
                        "..reporting..", "..system..", "..tenancy..", "..identity..")
                .because("core domain models must depend inward and remain independent of platform modules")
                .check(importedClasses);
    }

    @Test
    void identityAuthorizationDomainMustRemainFrameworkNeutral() {
        noClasses().that().resideInAPackage("..identity.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "..infrastructure..", "..adapters..")
                .because("authorization failures and identity rules belong to the pure domain")
                .check(importedClasses);
    }

    @Test
    void reportingMustNotAccessOperationalPersistence() {
        noClasses().that().resideInAPackage("..reporting..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..fleet..persistence..", "..freight..persistence..", "..fuel..persistence..",
                        "..organization..persistence..", "..routing..persistence..", "..trip..persistence..",
                        "..delivery..persistence..", "..notification..persistence..", "..offlinesync..persistence..")
                .because("Reporting must consume public reporting contracts, never operational repositories or entities")
                .check(importedClasses);
    }

    @Test
    void onlyApprovedModuleDependenciesAreAllowed() {
        classes().should(useOnlyApprovedModuleDependencies()).check(importedClasses);
    }

    @Test
    void crossModuleDependenciesMustTargetPublishedContracts() {
        classes().should(targetOnlyPublishedModuleContracts()).check(importedClasses);
    }

    @Test
    void tenantOwnedJpaEntitiesMustUseTheCentralTenantDiscriminator() {
        classes().that().areAnnotatedWith(Entity.class)
                .and().resideOutsideOfPackages("..identity..", "..tenancy..")
                .and().doNotHaveSimpleName("NotificationTemplateEntity")
                .should().beAssignableTo(TenantScopedEntity.class)
                .because("tenant-owned persistence must be protected from unscoped inherited repository operations")
                .check(importedClasses);
    }

    @Test
    void inboundRequestDtosMustNotAcceptTenantAuthority() {
        noClasses().that().resideInAnyPackage("..web.dto.request..", "..web.request..")
                .should(declareFieldNamed("tenantId"))
                .because("client-supplied tenant identifiers are never authorization evidence")
                .check(importedClasses);
    }

    @Test
    void moduleCodeMustUseTransactionAwareEventPublicationContract() {
        noClasses().that().resideOutsideOfPackage("..shared.infrastructure.events..")
                .should().dependOnClassesThat().haveFullyQualifiedName(
                        "org.springframework.context.ApplicationEventPublisher")
                .because("local secondary reactions must be deferred until the owning transaction commits")
                .check(importedClasses);
    }

    private static ArchCondition<JavaClass> declareFieldNamed(String fieldName) {
        return new ArchCondition<>("declare field named " + fieldName) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (item.getFields().stream().anyMatch(field -> field.getName().equals(fieldName))) {
                    events.add(SimpleConditionEvent.satisfied(item,
                            item.getName() + " declares client-controlled field " + fieldName));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnForeignTypeAssignableTo(
            Class<?> type, String description) {
        return foreignDependencyCondition(description, target -> target.isAssignableTo(type));
    }

    private static ArchCondition<JavaClass> notDependOnForeignTypeAnnotatedWith(
            Class<? extends Annotation> annotation, String description) {
        return foreignDependencyCondition(description, target -> target.isAnnotatedWith(annotation));
    }

    private static ArchCondition<JavaClass> foreignDependencyCondition(
            String description, java.util.function.Predicate<JavaClass> prohibitedTarget) {
        return new ArchCondition<>("not depend on a foreign module's " + description) {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String sourceModule = moduleOf(source);
                if (sourceModule == null) {
                    return;
                }
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetModule = moduleOf(target);
                    if (targetModule != null && !sourceModule.equals(targetModule) && prohibitedTarget.test(target)) {
                        events.add(SimpleConditionEvent.violated(source, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> useOnlyApprovedModuleDependencies() {
        return new ArchCondition<>("use only approved module dependencies") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String sourceModule = moduleOf(source);
                if (sourceModule == null) {
                    return;
                }
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    String targetModule = moduleOf(dependency.getTargetClass());
                    if (targetModule == null || sourceModule.equals(targetModule) || "shared".equals(targetModule)) {
                        continue;
                    }
                    String edge = sourceModule + "->" + targetModule;
                    if (!APPROVED_MODULE_DEPENDENCIES.contains(edge)) {
                        events.add(SimpleConditionEvent.violated(source,
                                dependency.getDescription() + " creates module edge " + edge
                                        + " that is not in the P0-03 approved dependency graph"));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> targetOnlyPublishedModuleContracts() {
        return new ArchCondition<>("target only contracts published in a provider module's root package") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String sourceModule = moduleOf(source);
                if (sourceModule == null) {
                    return;
                }
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetModule = moduleOf(target);
                    if (targetModule == null || sourceModule.equals(targetModule) || "shared".equals(targetModule)) {
                        continue;
                    }
                    String publishedPackage = BASE_PACKAGE + "." + targetModule;
                    if (!target.getPackageName().equals(publishedPackage)) {
                        events.add(SimpleConditionEvent.violated(source,
                                dependency.getDescription() + " targets non-published package "
                                        + target.getPackageName()));
                    }
                }
            }
        };
    }

    private static String moduleOf(JavaClass javaClass) {
        String prefix = BASE_PACKAGE + ".";
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String remainder = packageName.substring(prefix.length());
        int separator = remainder.indexOf('.');
        return separator < 0 ? remainder : remainder.substring(0, separator);
    }
}
