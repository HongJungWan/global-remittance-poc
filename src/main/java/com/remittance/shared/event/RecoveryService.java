package com.remittance.shared.event;

import com.remittance.shared.outbox.OutboxEvent;
import com.remittance.shared.outbox.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * DLQ 이벤트 재처리 및 미처리 Outbox 이벤트 수동 릴레이를 위한 복구 서비스.
 */
@Service
public class RecoveryService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JdbcTemplate jdbcTemplate;

    public RecoveryService(OutboxEventRepository outboxEventRepository,
                           KafkaTemplate<String, String> kafkaTemplate,
                           JdbcTemplate jdbcTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 미처리 Outbox 이벤트를 Kafka로 수동 릴레이한다.
     * Debezium CDC가 정상 동작하지 않을 때 사용한다.
     */
    @Transactional
    public int relayUnprocessedOutboxEvents() {
        List<OutboxEvent> unprocessed = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        int count = 0;
        for (OutboxEvent event : unprocessed) {
            try {
                String topic = "outbox.event.manual-relay";
                kafkaTemplate.send(topic, event.getAggregateId().toString(), event.getPayload());
                event.markProcessed();
                outboxEventRepository.save(event);
                count++;
                log.info("Outbox event relayed: id={}, type={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Failed to relay outbox event: id={}", event.getId(), e);
            }
        }
        return count;
    }

    /**
     * 특정 이벤트 ID의 멱등성 기록을 삭제하여 재처리를 허용한다.
     */
    @Transactional
    public void resetProcessedEvent(String schema, UUID eventId) {
        String sql = String.format("DELETE FROM %s.processed_events WHERE event_id = ?", schema);
        jdbcTemplate.update(sql, eventId);
        log.info("Processed event reset: schema={}, eventId={}", schema, eventId);
    }
}
