#!/bin/bash
# Script to create Kafka topics for EDRS system

KAFKA_CONTAINER="edrs-kafka"
KAFKA_BOOTSTRAP="localhost:9092"

echo "Waiting for Kafka to be ready..."
until docker exec $KAFKA_CONTAINER kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; do
  echo "Waiting for Kafka..."
  sleep 2
done

echo "Kafka is ready. Creating topics..."

# Create topics with replication factor 1 and 3 partitions (suitable for development)
# In production, use replication factor 3+ and adjust partitions based on load

docker exec $KAFKA_CONTAINER kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic reservation-requested \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

docker exec $KAFKA_CONTAINER kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic cancellation-requested \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

docker exec $KAFKA_CONTAINER kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic reservation-created \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

docker exec $KAFKA_CONTAINER kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic reservation-failed \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

docker exec $KAFKA_CONTAINER kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic cancellation-successful \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

docker exec $KAFKA_CONTAINER kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic inventory-received \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

echo "Topics created. Listing all topics:"
docker exec $KAFKA_CONTAINER kafka-topics --list --bootstrap-server localhost:9092
