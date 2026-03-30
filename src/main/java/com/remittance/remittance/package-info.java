/**
 * Remittance 모듈 (핵심 도메인).
 * 환율 적용, 송금 지시, 상태 추적, 전체 오케스트레이션.
 * 스키마: fintech_remittance
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = "shared"
)
package com.remittance.remittance;
