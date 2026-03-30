package com.remittance.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record QuoteLockedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        BigDecimal rate,
        Instant expiresAt
) implements DomainEvent {

    public QuoteLockedEvent(UUID orderId, BigDecimal rate, Instant expiresAt) {
        this(UUID.randomUUID(), Instant.now(), orderId, rate, expiresAt);
    }
}
