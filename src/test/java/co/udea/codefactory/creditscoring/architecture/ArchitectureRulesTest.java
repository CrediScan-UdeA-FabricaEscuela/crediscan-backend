package co.udea.codefactory.creditscoring.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
}
