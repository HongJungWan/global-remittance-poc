package com.remittance.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 모듈 격리 테스트.
 * 각 모듈은 자기 패키지 + shared 패키지만 참조 가능하며,
 * 다른 도메인 모듈을 직접 참조할 수 없다.
 */
class ModuleIsolationTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.remittance");
    }

    @Test
    @DisplayName("User 모듈은 다른 도메인 모듈(payment, remittance, partner)을 참조할 수 없다")
    void userModuleShouldNotDependOnOtherDomainModules() {
        noClasses()
                .that().resideInAPackage("com.remittance.user..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.remittance.payment..",
                        "com.remittance.remittance..",
                        "com.remittance.partner.."
                )
                .allowEmptyShould(true)
                .because("모듈 간 직접 참조 금지 — 이벤트 기반 통신만 허용")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Payment 모듈은 다른 도메인 모듈(user, remittance, partner)을 참조할 수 없다")
    void paymentModuleShouldNotDependOnOtherDomainModules() {
        noClasses()
                .that().resideInAPackage("com.remittance.payment..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.remittance.user..",
                        "com.remittance.remittance..",
                        "com.remittance.partner.."
                )
                .allowEmptyShould(true)
                .because("모듈 간 직접 참조 금지 — 이벤트 기반 통신만 허용")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Remittance 모듈은 다른 도메인 모듈(user, payment, partner)을 참조할 수 없다")
    void remittanceModuleShouldNotDependOnOtherDomainModules() {
        noClasses()
                .that().resideInAPackage("com.remittance.remittance..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.remittance.user..",
                        "com.remittance.payment..",
                        "com.remittance.partner.."
                )
                .allowEmptyShould(true)
                .because("모듈 간 직접 참조 금지 — 이벤트 기반 통신만 허용")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Partner 모듈은 다른 도메인 모듈(user, payment, remittance)을 참조할 수 없다")
    void partnerModuleShouldNotDependOnOtherDomainModules() {
        noClasses()
                .that().resideInAPackage("com.remittance.partner..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.remittance.user..",
                        "com.remittance.payment..",
                        "com.remittance.remittance.."
                )
                .allowEmptyShould(true)
                .because("모듈 간 직접 참조 금지 — 이벤트 기반 통신만 허용")
                .check(importedClasses);
    }

    @Test
    @DisplayName("shared 모듈은 도메인 모듈에 의존하지 않는다 (역방향 의존 금지)")
    void sharedShouldNotDependOnDomainModules() {
        noClasses()
                .that().resideInAPackage("com.remittance.shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.remittance.user..",
                        "com.remittance.payment..",
                        "com.remittance.remittance..",
                        "com.remittance.partner.."
                )
                .allowEmptyShould(true)
                .because("shared 모듈은 도메인 모듈에 의존하지 않는다 (역방향 의존 금지)")
                .check(importedClasses);
    }
}
