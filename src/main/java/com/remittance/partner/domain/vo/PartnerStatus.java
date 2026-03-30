package com.remittance.partner.domain.vo;

public enum PartnerStatus {
    PROCESSING,
    COMPLETED,
    FAILED,
    TIMEOUT;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == TIMEOUT;
    }
}
