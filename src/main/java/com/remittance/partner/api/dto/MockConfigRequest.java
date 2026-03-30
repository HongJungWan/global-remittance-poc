package com.remittance.partner.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MockConfigRequest(
        @NotNull String mode,
        @Min(0) @Max(30000) long delayMs,
        @Min(0) @Max(100) int failurePercent
) {
}
