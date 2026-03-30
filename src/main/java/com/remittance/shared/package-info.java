/**
 * Shared 모듈.
 * 모든 도메인 모듈이 공유하는 인프라: 이벤트, Outbox, 분산 락, 설정.
 */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.remittance.shared;
