package com.remittance.shared.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DomainEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    @DisplayName("UserCreatedEvent - 편의 생성자로 eventId와 occurredAt이 자동 생성된다")
    void userCreatedEvent_autoGeneratesMetadata() {
        UUID userId = UUID.randomUUID();
        var event = new UserCreatedEvent(userId, "홍길동", "PENDING");

        assertNotNull(event.eventId());
        assertNotNull(event.occurredAt());
        assertEquals(userId, event.userId());
        assertEquals("홍길동", event.displayName());
        assertEquals("PENDING", event.kycStatus());
        assertEquals("UserCreatedEvent", event.eventType());
    }

    @Test
    @DisplayName("RemittanceCreatedEvent - 금액과 통화가 정확히 보존된다")
    void remittanceCreatedEvent_preservesAmountAndCurrency() {
        UUID orderId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        var event = new RemittanceCreatedEvent(orderId, senderId, new BigDecimal("1000000.50"), "KRW");

        assertEquals(orderId, event.orderId());
        assertEquals(senderId, event.senderId());
        assertEquals(new BigDecimal("1000000.50"), event.amount());
        assertEquals("KRW", event.currency());
    }

    @Test
    @DisplayName("QuoteLockedEvent - 환율과 만료 시각이 정확히 저장된다")
    void quoteLockedEvent_storesRateAndExpiry() {
        UUID orderId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(30);
        var event = new QuoteLockedEvent(orderId, new BigDecimal("1350.12345678"), expiresAt);

        assertEquals(new BigDecimal("1350.12345678"), event.rate());
        assertEquals(expiresAt, event.expiresAt());
    }

    @Test
    @DisplayName("PaymentRequestedEvent - 결제 메서드 정보가 포함된다")
    void paymentRequestedEvent_includesPaymentMethod() {
        var event = new PaymentRequestedEvent(
                UUID.randomUUID(), new BigDecimal("500.00"), "USD", "CREDIT_CARD");

        assertEquals("CREDIT_CARD", event.paymentMethod());
        assertEquals("PaymentRequestedEvent", event.eventType());
    }

    @Test
    @DisplayName("PartnerProcessingRequestedEvent - 수취 정보가 포함된다")
    void partnerProcessingRequestedEvent_includesReceiverInfo() {
        var event = new PartnerProcessingRequestedEvent(
                UUID.randomUUID(), "PARTNER_A", new BigDecimal("100.00"), "PHP", "receiver-json");

        assertEquals("PARTNER_A", event.partnerCode());
        assertEquals("PHP", event.targetCurrency());
        assertEquals("receiver-json", event.receiverInfo());
    }

    @Test
    @DisplayName("모든 이벤트는 JSON 직렬화/역직렬화가 가능하다")
    void allEvents_canBeSerializedToJson() throws Exception {
        var event = new PaymentCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "TXN-12345");

        String json = objectMapper.writeValueAsString(event);
        assertNotNull(json);
        assertTrue(json.contains("TXN-12345"));

        PaymentCompletedEvent deserialized = objectMapper.readValue(json, PaymentCompletedEvent.class);
        assertEquals(event.orderId(), deserialized.orderId());
        assertEquals(event.transactionRef(), deserialized.transactionRef());
    }

    @Test
    @DisplayName("동일 파라미터로 생성된 이벤트는 서로 다른 eventId를 가진다")
    void events_haveDifferentEventIds() {
        UUID orderId = UUID.randomUUID();
        var event1 = new QuoteExpiredEvent(orderId);
        var event2 = new QuoteExpiredEvent(orderId);

        assertNotEquals(event1.eventId(), event2.eventId());
    }

    @Test
    @DisplayName("PartnerFailedEvent - 실패 사유가 포함된다")
    void partnerFailedEvent_includesFailureReason() {
        var event = new PartnerFailedEvent(
                UUID.randomUUID(), "PTX-999", "Connection timeout");

        assertEquals("PTX-999", event.partnerTransactionId());
        assertEquals("Connection timeout", event.failureReason());
        assertEquals("PartnerFailedEvent", event.eventType());
    }
}
