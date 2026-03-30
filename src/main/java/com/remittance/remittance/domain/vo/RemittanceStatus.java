package com.remittance.remittance.domain.vo;

public enum RemittanceStatus {
    CREATED,
    QUOTE_LOCKED,
    QUOTE_EXPIRED,
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    COMPLIANCE_CHECK,
    COMPLIANCE_REJECTED,
    PARTNER_PROCESSING,
    PARTNER_FAILED,
    REFUND_PENDING,
    REFUNDED,
    CANCELLED,
    COMPLETED;

    public boolean isTerminal() {
        return this == QUOTE_EXPIRED || this == CANCELLED || this == REFUNDED || this == COMPLETED;
    }
}
