package com.remittance.shared.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID paymentId,
        String transactionRef
) implements DomainEvent {

    public PaymentCompletedEvent(UUID orderId, UUID paymentId, String transactionRef) {
        this(UUID.randomUUID(), Instant.now(), orderId, paymentId, transactionRef);
    }
}
