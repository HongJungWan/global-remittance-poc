package com.remittance.remittance.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateQuoteRequest(
        @NotBlank String receiverName,
        @NotBlank String receiverAccount,
        @NotBlank String receiverBankCode,
        @NotBlank @Size(min = 2, max = 3) String receiverCountry,
        @NotBlank @Size(min = 3, max = 3) String sourceCurrency,
        @NotBlank @Size(min = 3, max = 3) String targetCurrency,
        @NotNull @DecimalMin("0.01") BigDecimal sourceAmount,
        @NotBlank String paymentMethod
) {
}
