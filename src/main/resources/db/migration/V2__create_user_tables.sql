-- V2: User & Auth 도메인 테이블
CREATE TABLE fintech_user.users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    kyc_status    VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    role          VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
