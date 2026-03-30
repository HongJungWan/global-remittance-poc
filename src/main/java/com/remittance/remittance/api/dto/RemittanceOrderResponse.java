package com.remittance.remittance.api.dto;

import com.remittance.remittance.domain.RemittanceOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RemittanceOrderResponse(
        UUID orderId,
        UUID senderId,
        String receiverName,
        String receiverCountry,
        String sourceCurrency,
        String targetCurrency,
        BigDecimal sourceAmount,
        BigDecimal targetAmount,
        BigDecimal exchangeRate,
        String status,
        Instant quoteExpiresAt,
        Instant createdAt
) {
    public static RemittanceOrderResponse from(RemittanceOrder order) {
        return new RemittanceOrderResponse(
                order.getId(),
                order.getSenderId(),
                order.getReceiverInfo().getName(),
                order.getReceiverInfo().getCountry(),
                order.getSourceCurrency(),
                order.getTargetCurrency(),
                order.getSourceAmount(),
                order.getTargetAmount(),
                order.getExchangeRate(),
                order.getStatus().name(),
                order.getQuoteExpiresAt(),
                order.getCreatedAt()
        );
    }
}
