package com.remittance.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RemittanceCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID senderId,
        BigDecimal amount,
        String currency
) implements DomainEvent {

    public RemittanceCreatedEvent(UUID orderId, UUID senderId, BigDecimal amount, String currency) {
        this(UUID.randomUUID(), Instant.now(), orderId, senderId, amount, currency);
    }
}
