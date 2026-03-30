/**
 * Payment 모듈.
 * 송금을 위한 자산 Funding 및 결제 트랜잭션 처리.
 * 스키마: fintech_payment
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = "shared"
)
package com.remittance.payment;
