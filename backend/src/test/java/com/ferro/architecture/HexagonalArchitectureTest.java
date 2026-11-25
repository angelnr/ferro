package com.ferro.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import org.springframework.beans.factory.annotation.Autowired;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Reglas de acoplamiento de docs/04-arquitectura.md §4.
 *
 * <p>Son gates de build desde M0 (RNF-44). Si una regla falla, el diseño se corrige; no se
 * relaja la regla sin ADR.
 */
@AnalyzeClasses(packages = "com.ferro", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domain_is_free_of_frameworks_and_outer_layers = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.validation..",
                    "com.fasterxml.jackson..",
                    "tools.jackson..",
                    "..application..",
                    "..infrastructure..",
                    "..interfaces..",
                    "..api..");

    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..", "..interfaces..");

    @ArchTest
    static final ArchRule interfaces_do_not_leak_into_domain_or_application = noClasses()
            .that()
            .resideInAnyPackage("..domain..", "..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..interfaces..");

    @ArchTest
    static final ArchRule shared_kernel_does_not_depend_on_business_modules = noClasses()
            .that()
            .resideInAPackage("com.ferro.shared..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.ferro.identity..",
                    "com.ferro.catalog..",
                    "com.ferro.training..",
                    "com.ferro.progress..",
                    "com.ferro.body..",
                    "com.ferro.platform..");

    @ArchTest
    static final ArchRule no_field_injection =
            noFields().should().beAnnotatedWith(Autowired.class).because("usa inyección por constructor (ADR-0002)");

    // M0 aún no tiene controladores REST. La regla se activa sola en cuanto exista el primero
    // con ese sufijo; allowEmptyShould evita un falso rojo del esqueleto, no relaja la regla.
    @ArchTest
    static final ArchRule rest_controllers_live_in_interfaces = classes()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .resideInAPackage("..interfaces..")
            .because("los controladores son adaptadores de entrada, no dominio ni aplicación")
            .allowEmptyShould(true);
}
