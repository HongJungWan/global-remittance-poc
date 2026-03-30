-- V6: Transactional Outbox + 멱등성 보장 테이블 (각 스키마 공통)

-- fintech_user
CREATE TABLE fintech_user.outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    processed       BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_user_outbox_unprocessed
    ON fintech_user.outbox_events (processed, created_at) WHERE processed = FALSE;

CREATE TABLE fintech_user.processed_events (
    event_id      UUID PRIMARY KEY,
    processed_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- fintech_payment
CREATE TABLE fintech_payment.outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    processed       BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_payment_outbox_unprocessed
    ON fintech_payment.outbox_events (processed, created_at) WHERE processed = FALSE;

CREATE TABLE fintech_payment.processed_events (
    event_id      UUID PRIMARY KEY,
    processed_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- fintech_remittance
CREATE TABLE fintech_remittance.outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    processed       BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_remittance_outbox_unprocessed
    ON fintech_remittance.outbox_events (processed, created_at) WHERE processed = FALSE;

CREATE TABLE fintech_remittance.processed_events (
    event_id      UUID PRIMARY KEY,
    processed_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- fintech_partner
CREATE TABLE fintech_partner.outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    processed       BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_partner_outbox_unprocessed
    ON fintech_partner.outbox_events (processed, created_at) WHERE processed = FALSE;

CREATE TABLE fintech_partner.processed_events (
    event_id      UUID PRIMARY KEY,
    processed_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
