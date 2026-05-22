package co.udea.codefactory.creditscoring.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * Architecture enforcement tests using ArchUnit.
 *
 * <p>These tests block merges that violate the hexagonal architecture contract:
 * <ul>
 *   <li>Domain MUST NOT import from infrastructure or Spring/JPA frameworks</li>
 *   <li>Application MUST NOT import from infrastructure adapters</li>
 *   <li>Domain MUST NOT directly depend on other bounded contexts' domain internals</li>
 * </ul>
 */
class ArchitectureRulesTest {

    private static final String ROOT_PACKAGE = "co.udea.codefactory.creditscoring";
    private static JavaClasses classes;

    @BeforeAll
    static void loadClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT_PACKAGE);
    }

    @Test
    void domain_should_not_import_infrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .as("Domain must not depend on infrastructure adapters");
        rule.check(classes);
    }

    @Test
    void domain_should_not_import_spring_framework() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .and().resideOutsideOfPackage("..shared.exception..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework..")
                .as("Domain must not import Spring framework classes (use shared types instead)");
        rule.check(classes);
    }

    @Test
    void domain_should_not_import_jpa_annotations() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("jakarta.persistence..")
                .as("Domain must not import JPA annotations");
        rule.check(classes);
    }

    @Test
    void application_should_not_import_infrastructure_adapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .as("Application services must depend on ports (interfaces), not infrastructure implementations");
        rule.check(classes);
    }

    @Test
    void controllers_should_not_use_out_ports() {
        // Rendering/export ports (Generar*) are exempt: controllers legitimately
        // call them to convert domain data into HTTP response bytes (PDF/CSV).
        DescribedPredicate<JavaClass> nonRenderingOutPort =
            new DescribedPredicate<JavaClass>("out-port (excluding Generar* rendering/export ports)") {
                @Override
                public boolean test(JavaClass c) {
                    return c.getPackageName().contains(".domain.port.out")
                        && !c.getSimpleName().contains("Generar");
                }
            };
        noClasses()
            .that().resideInAPackage("..infrastructure.adapter.in.rest..")
            .should().dependOnClassesThat(nonRenderingOutPort)
            .check(classes);
    }

    @Test
    void rest_adapters_should_not_import_persistence_adapters() {
        noClasses()
            .that().resideInAPackage("..infrastructure.adapter.in.rest..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure.adapter.out.persistence..")
            .check(classes);
    }

    @Test
    void transactional_class_annotation_only_in_application_services() {
        noClasses()
            .that().resideOutsideOfPackages(
                "..application.service..",
                "..application.util..")
            .and().haveSimpleNameNotContaining("Test")
            .should().beAnnotatedWith(Transactional.class)
            .check(classes);
    }

    @Test
    void transactional_method_annotation_only_in_application_services() {
        noMethods()
            .that().areDeclaredInClassesThat().resideOutsideOfPackages(
                "..application.service..",
                "..application.util..")
            .and().areDeclaredInClassesThat().haveSimpleNameNotContaining("Test")
            .should().beAnnotatedWith(Transactional.class)
            .check(classes);
    }
}
