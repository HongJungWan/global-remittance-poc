-- V3: Remittance 도메인 테이블
CREATE TABLE fintech_remittance.remittance_orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id           UUID NOT NULL,
    receiver_name       VARCHAR(100) NOT NULL,
    receiver_account    VARCHAR(50) NOT NULL,
    receiver_bank_code  VARCHAR(20) NOT NULL,
    receiver_country    VARCHAR(3) NOT NULL,
    source_currency     VARCHAR(3) NOT NULL,
    target_currency     VARCHAR(3) NOT NULL,
    source_amount       DECIMAL(18,2) NOT NULL,
    target_amount       DECIMAL(18,2) NOT NULL,
    exchange_rate       DECIMAL(18,8) NOT NULL,
    quote_expires_at    TIMESTAMP,
    status              VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_remittance_sender ON fintech_remittance.remittance_orders (sender_id);
CREATE INDEX idx_remittance_status ON fintech_remittance.remittance_orders (status);
CREATE INDEX idx_remittance_created ON fintech_remittance.remittance_orders (created_at);

-- CQRS Read Model: User 스냅샷
CREATE TABLE fintech_remittance.user_snapshots (
    user_id       UUID PRIMARY KEY,
    display_name  VARCHAR(100) NOT NULL,
    kyc_status    VARCHAR(20) NOT NULL,
    updated_at    TIMESTAMP NOT NULL
);
