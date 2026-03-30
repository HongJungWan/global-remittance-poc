package com.remittance.payment.infrastructure.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remittance.payment.application.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Remittance 모듈의 PaymentRequestedEvent를 구독하여 결제를 처리한다.
 */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public PaymentEventListener(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "outbox.event.fintech_remittance", groupId = "payment-remittance-events")
    public void handleRemittanceEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";

            if ("PaymentRequestedEvent".equals(eventType)) {
                UUID orderId = UUID.fromString(node.get("orderId").asText());
                BigDecimal amount = new BigDecimal(node.get("amount").asText());
                String currency = node.get("currency").asText();
                String paymentMethod = node.get("paymentMethod").asText();

                paymentService.processPayment(orderId, amount, currency, paymentMethod);
                log.debug("PaymentRequestedEvent processed: orderId={}", orderId);
            }
        } catch (Exception e) {
            log.error("Failed to process remittance event: {}", message, e);
        }
    }
}
