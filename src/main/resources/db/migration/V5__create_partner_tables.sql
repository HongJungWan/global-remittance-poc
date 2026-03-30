-- V5: Partner Integration 도메인 테이블
CREATE TABLE fintech_partner.partner_transactions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    remittance_id           UUID NOT NULL,
    partner_code            VARCHAR(30) NOT NULL,
    partner_transaction_id  VARCHAR(100),
    amount                  DECIMAL(18,2) NOT NULL,
    currency                VARCHAR(3) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    failure_reason          TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
