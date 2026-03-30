package com.remittance.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 레이어 의존성 테스트.
 * DDD 레이어 규칙을 강제하여 domain 순수성을 보장한다.
 */
class LayerDependencyTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.remittance");
    }

    @Test
    @DisplayName("domain 패키지는 infrastructure, api 패키지를 참조할 수 없다")
    void domainShouldNotDependOnInfrastructureOrApi() {
        noClasses()
                .that().resideInAnyPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..infrastructure..",
                        "..api.."
                )
                .allowEmptyShould(true)
                .because("도메인 레이어는 인프라/API 레이어에 의존하지 않는다 (DDD 원칙)")
                .check(importedClasses);
    }

    @Test
    @DisplayName("api 패키지는 infrastructure 패키지를 직접 참조할 수 없다")
    void apiShouldNotDirectlyDependOnInfrastructure() {
        noClasses()
                .that().resideInAnyPackage("..api..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..infrastructure.."
                )
                .allowEmptyShould(true)
                .because("API 레이어는 application 레이어를 통해서만 인프라에 접근한다")
                .check(importedClasses);
    }

    @Test
    @DisplayName("@Entity 클래스는 domain, outbox 또는 shared event 패키지에만 위치해야 한다")
    void entitiesShouldResideInDomainPackage() {
        classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAnyPackage("..domain..", "..outbox..", "..shared.event..")
                .allowEmptyShould(true)
                .because("JPA 엔티티는 도메인 패키지에 위치한다 (Outbox/ProcessedEvent는 shared 인프라 예외)")
                .check(importedClasses);
    }
}
