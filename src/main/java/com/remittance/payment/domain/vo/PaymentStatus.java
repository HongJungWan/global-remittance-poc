package com.remittance.payment.domain.vo;

public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == REFUNDED;
    }
}
