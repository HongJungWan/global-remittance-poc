package com.remittance.remittance.infrastructure.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remittance.remittance.domain.UserSnapshot;
import com.remittance.remittance.infrastructure.UserSnapshotRepository;
import com.remittance.shared.event.IdempotencyChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * User 모듈의 이벤트를 구독하여 UserSnapshot CQRS Read Model을 유지한다.
 * 멱등성: processed_events 테이블로 eventId 중복 처리를 방지한다.
 */
@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);
    private static final String SCHEMA = "fintech_remittance";

    private final UserSnapshotRepository userSnapshotRepository;
    private final IdempotencyChecker idempotencyChecker;
    private final ObjectMapper objectMapper;

    public UserEventListener(UserSnapshotRepository userSnapshotRepository,
                             IdempotencyChecker idempotencyChecker,
                             ObjectMapper objectMapper) {
        this.userSnapshotRepository = userSnapshotRepository;
        this.idempotencyChecker = idempotencyChecker;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "outbox.event.fintech_user", groupId = "remittance-user-snapshot")
    @Transactional
    public void handleUserEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";

            if (!"UserCreatedEvent".equals(eventType) && !"UserUpdatedEvent".equals(eventType)) {
                return;
            }

            UUID eventId = UUID.fromString(node.get("eventId").asText());
            if (idempotencyChecker.isAlreadyProcessed(SCHEMA, eventId)) {
                log.debug("Duplicate event skipped: eventId={}, type={}", eventId, eventType);
                return;
            }

            UUID userId = UUID.fromString(node.get("userId").asText());
            String displayName = node.get("displayName").asText();
            String kycStatus = node.get("kycStatus").asText();

            userSnapshotRepository.findById(userId)
                    .ifPresentOrElse(
                            snapshot -> snapshot.update(displayName, kycStatus),
                            () -> userSnapshotRepository.save(
                                    new UserSnapshot(userId, displayName, kycStatus))
                    );

            idempotencyChecker.markProcessed(SCHEMA, eventId);
            log.debug("UserSnapshot upserted: userId={}, event={}", userId, eventType);
        } catch (Exception e) {
            log.error("Failed to process user event: {}", message, e);
            throw new RuntimeException("User event processing failed", e);
        }
    }
}
