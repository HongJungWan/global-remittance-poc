/**
 * User & Auth 모듈.
 * 고객 계정 관리, JWT 인증/인가, KYC 상태 관리.
 * 스키마: fintech_user
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = "shared"
)
package com.remittance.user;
