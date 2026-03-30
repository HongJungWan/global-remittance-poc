-- V4: Payment 도메인 테이블
CREATE TABLE fintech_payment.payments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    remittance_id     UUID NOT NULL,
    amount            DECIMAL(18,2) NOT NULL,
    currency          VARCHAR(3) NOT NULL,
    payment_method    VARCHAR(30) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_ref   VARCHAR(100),
    failure_reason    TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
