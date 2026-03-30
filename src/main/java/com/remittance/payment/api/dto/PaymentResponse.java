package com.remittance.payment.api.dto;

import com.remittance.payment.domain.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID remittanceId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String status,
        String transactionRef,
        String failureReason,
        Instant createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getRemittanceId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod().name(),
                payment.getStatus().name(),
                payment.getTransactionRef(),
                payment.getFailureReason(),
                payment.getCreatedAt()
        );
    }
}
