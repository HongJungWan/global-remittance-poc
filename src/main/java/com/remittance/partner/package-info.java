/**
 * Partner Integration 모듈.
 * 외부 파트너사 API 통신을 격리하는 Anti-Corruption Layer(ACL).
 * 스키마: fintech_partner
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = "shared"
)
package com.remittance.partner;
