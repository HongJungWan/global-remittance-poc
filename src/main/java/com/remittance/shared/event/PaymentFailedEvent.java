package com.remittance.shared.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID paymentId,
        String failureReason
) implements DomainEvent {

    public PaymentFailedEvent(UUID orderId, UUID paymentId, String failureReason) {
        this(UUID.randomUUID(), Instant.now(), orderId, paymentId, failureReason);
    }
}
