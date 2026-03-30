# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
./gradlew clean build          # Full build + all tests
./gradlew test                 # Run all tests
./gradlew test --tests "com.remittance.architecture.*"   # ArchUnit only
./gradlew test --tests "com.remittance.acceptance.*"     # Acceptance tests only
./gradlew test --tests "com.remittance.user.domain.UserTest"  # Single test class
```

- Java 21, Spring Boot 3.4.3, Gradle 8.12
- Tests use H2 in PostgreSQL mode (no Docker required). Profile: `test`
- Kafka, Redis, Redisson auto-configuration are excluded in acceptance tests via `@TestPropertySource`

## Architecture

**Modular monolith** with 4 bounded contexts + shared module. Modules communicate only via events; direct imports between domain modules are forbidden and enforced by ArchUnit in CI.

```
com.remittance.user        → fintech_user schema       (JWT auth, KYC)
com.remittance.payment     → fintech_payment schema    (payment processing)
com.remittance.remittance  → fintech_remittance schema (core: state machine, FX, CQRS)
com.remittance.partner     → fintech_partner schema    (ACL, circuit breaker, mock partner)
com.remittance.shared      → cross-cutting (events, outbox, locks, config)
```

Each module follows DDD layers: `domain/` → `application/` → `infrastructure/` → `api/`

## Critical Constraints (ArchUnit-enforced)

- **No cross-module imports**: `user` cannot import from `payment`, `remittance`, or `partner` (and vice versa). Only `shared` is allowed.
- **Domain purity**: `domain` packages cannot depend on `infrastructure` or `api`
- **API isolation**: `api` packages cannot directly reference `infrastructure` (must go through `application`)
- **@Entity placement**: Only in `..domain..`, `..outbox..`, or `..shared.event..` packages

## Key Patterns

**Transactional Outbox**: `OutboxEventPublisher` uses `JdbcTemplate` with dynamic schema routing (`INSERT INTO {schema}.outbox_events`). Called within `@Transactional` business methods. Debezium CDC relays to Kafka.

**Consumer Idempotency**: `IdempotencyChecker` checks `processed_events` table before processing, marks after. All 4 Kafka listeners apply this pattern.

**State Machine**: `RemittanceOrder` has 14 states with transitions enforced inside entity methods. Invalid transitions throw `IllegalStateException`. Quote Lock-in has 30-second TTL.

**Distributed Lock**: `DistributedLockManager` interface with Redisson impl (production) and NoOp impl (tests). Lock key: `remittance:account:{accountId}`. Non-blocking tryLock with immediate rejection.

**DLQ**: `KafkaConfig` routes failed messages (after 3 attempts) to `{topic}.dlq` via `DeadLetterPublishingRecoverer`.

## Schema-per-Module

Each module owns its PostgreSQL schema. Flyway migrations V1–V6. Each schema has its own `outbox_events` and `processed_events` tables. No cross-schema JOINs allowed — use CQRS snapshots instead (e.g., `UserSnapshot` in remittance module).

## Test Infrastructure

- `AcceptanceTestBase`: Base class for `@SpringBootTest` acceptance tests. Excludes Kafka/Redis/Redisson auto-config. Mocks `RedisTemplate`, `KafkaTemplate`, `OutboxEventPublisher`.
- `application-test.yml`: H2 with `INIT=CREATE SCHEMA IF NOT EXISTS fintech_*` for all 4 schemas. `spring.kafka.enabled: false`.
- `KafkaConfig` has `@ConditionalOnProperty(name = "spring.kafka.enabled")`, `RedisConfig` has `@ConditionalOnBean(RedisConnectionFactory.class)` — both skip gracefully in tests.
- `NoOpDistributedLockManager` activates when `RedissonClient` bean is absent.
