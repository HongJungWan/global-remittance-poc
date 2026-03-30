package com.remittance.shared.event;

import java.time.Instant;
import java.util.UUID;

public record QuoteExpiredEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId
) implements DomainEvent {

    public QuoteExpiredEvent(UUID orderId) {
        this(UUID.randomUUID(), Instant.now(), orderId);
    }
}
