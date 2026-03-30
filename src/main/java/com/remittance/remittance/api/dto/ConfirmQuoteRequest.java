package com.remittance.remittance.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmQuoteRequest(
        @NotBlank String paymentMethod
) {
}
