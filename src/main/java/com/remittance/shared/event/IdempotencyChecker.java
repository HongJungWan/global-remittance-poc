package com.remittance.shared.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * 이벤트 멱등성 검사 유틸리티.
 * 스키마별 processed_events 테이블을 사용하여 중복 이벤트를 필터링한다.
 */
@Component
public class IdempotencyChecker {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyChecker.class);

    private static final String CHECK_SQL = "SELECT COUNT(*) FROM %s.processed_events WHERE event_id = ?";
    private static final String INSERT_SQL = "INSERT INTO %s.processed_events (event_id, processed_at) VALUES (?, ?)";
    private static final String CHECK_SQL_H2 = "SELECT COUNT(*) FROM processed_events WHERE event_id = ?";
    private static final String INSERT_SQL_H2 = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public IdempotencyChecker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 이벤트가 이미 처리되었는지 확인한다.
     */
    public boolean isAlreadyProcessed(String schema, UUID eventId) {
        String sql = (schema == null || schema.isBlank()) ? CHECK_SQL_H2 : String.format(CHECK_SQL, schema);
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, eventId);
        return count != null && count > 0;
    }

    /**
     * 이벤트를 처리 완료로 기록한다.
     */
    public void markProcessed(String schema, UUID eventId) {
        String sql = (schema == null || schema.isBlank()) ? INSERT_SQL_H2 : String.format(INSERT_SQL, schema);
        jdbcTemplate.update(sql, eventId, Timestamp.from(Instant.now()));
        log.debug("Event marked as processed: schema={}, eventId={}", schema, eventId);
    }
}
