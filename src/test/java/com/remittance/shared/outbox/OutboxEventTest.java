package com.remittance.shared.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventTest {

    @Test
    @DisplayName("OutboxEvent 생성 시 필드가 정확히 설정된다")
    void create_setsAllFields() {
        UUID id = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        Instant now = Instant.now();
        String payload = "{\"orderId\":\"123\"}";

        OutboxEvent event = new OutboxEvent(
                id, "RemittanceOrder", aggregateId,
                "PaymentRequestedEvent", payload, now);

        assertEquals(id, event.getId());
        assertEquals("RemittanceOrder", event.getAggregateType());
        assertEquals(aggregateId, event.getAggregateId());
        assertEquals("PaymentRequestedEvent", event.getEventType());
        assertEquals(payload, event.getPayload());
        assertEquals(now, event.getCreatedAt());
        assertFalse(event.isProcessed());
    }

    @Test
    @DisplayName("markProcessed 호출 시 processed가 true로 변경된다")
    void markProcessed_setsProcessedTrue() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), "User", UUID.randomUUID(),
                "UserCreatedEvent", "{}", Instant.now());

        assertFalse(event.isProcessed());
        event.markProcessed();
        assertTrue(event.isProcessed());
    }

    @Test
    @DisplayName("새로 생성된 OutboxEvent는 항상 미처리 상태이다")
    void newEvent_isAlwaysUnprocessed() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), "Payment", UUID.randomUUID(),
                "PaymentCompletedEvent", "{\"amount\":100}", Instant.now());

        assertFalse(event.isProcessed());
    }
}
