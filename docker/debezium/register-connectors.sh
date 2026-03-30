#!/bin/bash
# Debezium CDC 커넥터 등록 스크립트
# Docker Compose 기동 후 Kafka Connect가 정상 작동할 때 실행

CONNECT_URL="http://localhost:8083"

echo "Waiting for Kafka Connect to be ready..."
until curl -s "$CONNECT_URL/connectors" > /dev/null 2>&1; do
    sleep 2
done
echo "Kafka Connect is ready."

# 각 모듈별 Outbox 테이블에 대한 Debezium 커넥터 등록
for schema in fintech_user fintech_payment fintech_remittance fintech_partner; do
    CONNECTOR_NAME="${schema}-outbox-connector"
    echo "Registering connector: $CONNECTOR_NAME"

    curl -s -X POST "$CONNECT_URL/connectors" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "'"$CONNECTOR_NAME"'",
            "config": {
                "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
                "database.hostname": "postgres",
                "database.port": "5432",
                "database.user": "remittance",
                "database.password": "remittance",
                "database.dbname": "remittance",
                "database.server.name": "remittance",
                "schema.include.list": "'"$schema"'",
                "table.include.list": "'"$schema"'.outbox_events",
                "transforms": "outbox",
                "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
                "transforms.outbox.table.fields.additional.placement": "event_type:header:eventType",
                "transforms.outbox.route.by.field": "aggregate_type",
                "transforms.outbox.route.topic.replacement": "outbox.event.'"$schema"'",
                "plugin.name": "pgoutput",
                "slot.name": "'"${schema}_slot"'",
                "publication.name": "'"${schema}_pub"'",
                "topic.prefix": "remittance"
            }
        }'
    echo ""
done

echo "All connectors registered."
curl -s "$CONNECT_URL/connectors" | python3 -m json.tool 2>/dev/null || curl -s "$CONNECT_URL/connectors"
