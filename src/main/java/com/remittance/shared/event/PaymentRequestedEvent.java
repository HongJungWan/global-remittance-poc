package com.remittance.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        BigDecimal amount,
        String currency,
        String paymentMethod
) implements DomainEvent {

    public PaymentRequestedEvent(UUID orderId, BigDecimal amount, String currency, String paymentMethod) {
        this(UUID.randomUUID(), Instant.now(), orderId, amount, currency, paymentMethod);
    }
}
