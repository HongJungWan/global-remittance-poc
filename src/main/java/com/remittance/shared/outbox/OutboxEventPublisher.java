package com.remittance.shared.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remittance.shared.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Outbox 이벤트 발행기.
 * 비즈니스 트랜잭션 내에서 호출되어, 같은 트랜잭션으로 Outbox 테이블에 INSERT한다.
 * JdbcTemplate을 사용하여 스키마를 동적으로 지정한다.
 */
@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private static final String INSERT_SQL =
            "INSERT INTO %s.outbox_events (id, aggregate_type, aggregate_id, event_type, payload, created_at, processed) "
                    + "VALUES (?, ?, ?, ?, ?::jsonb, ?, false)";

    private static final String INSERT_SQL_H2 =
            "INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload, created_at, processed) "
                    + "VALUES (?, ?, ?, ?, ?, ?, false)";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 도메인 이벤트를 지정된 스키마의 Outbox 테이블에 기록한다.
     *
     * @param schema        대상 스키마 (예: "fintech_user", "fintech_remittance")
     * @param aggregateType Aggregate Root 이름 (예: "User", "RemittanceOrder")
     * @param aggregateId   Aggregate ID
     * @param event         도메인 이벤트
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String schema, String aggregateType, UUID aggregateId, DomainEvent event) {
        String payload = serialize(event);
        String sql = resolveSql(schema);

        jdbcTemplate.update(sql,
                event.eventId(),
                aggregateType,
                aggregateId,
                event.eventType(),
                payload,
                Timestamp.from(event.occurredAt())
        );

        log.debug("Outbox event published: schema={}, type={}, aggregateId={}",
                schema, event.eventType(), aggregateId);
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize domain event: " + event.eventType(), e);
        }
    }

    private String resolveSql(String schema) {
        if (schema == null || schema.isBlank()) {
            return INSERT_SQL_H2;
        }
        return String.format(INSERT_SQL, schema);
    }
}
