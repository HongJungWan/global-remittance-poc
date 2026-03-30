package com.remittance.partner.api.dto;

public record MockConfigResponse(
        String mode,
        long delayMs,
        int failurePercent
) {
}
