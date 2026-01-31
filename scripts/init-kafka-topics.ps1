# PowerShell script to create Kafka topics for EDRS system

$KAFKA_CONTAINER = "edrs-kafka"
$KAFKA_BOOTSTRAP = "localhost:9092"

Write-Host "Waiting for Kafka to be ready..."
do {
    Start-Sleep -Seconds 2
    $result = docker exec $KAFKA_CONTAINER kafka-broker-api-versions --bootstrap-server localhost:9092 2>&1
} while ($LASTEXITCODE -ne 0)

Write-Host "Kafka is ready. Creating topics..."

# Create topics with replication factor 1 and 3 partitions (suitable for development)
$topics = @(
    "reservation-requested",
    "cancellation-requested",
    "reservation-created",
    "reservation-failed",
    "cancellation-successful",
    "inventory-received"
)

foreach ($topic in $topics) {
    Write-Host "Creating topic: $topic"
    docker exec $KAFKA_CONTAINER kafka-topics --create `
        --bootstrap-server localhost:9092 `
        --topic $topic `
        --partitions 3 `
        --replication-factor 1 `
        --if-not-exists
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Topic $topic created successfully" -ForegroundColor Green
    } else {
        Write-Host "Failed to create topic $topic" -ForegroundColor Red
    }
}

Write-Host "`nListing all topics:"
docker exec $KAFKA_CONTAINER kafka-topics --list --bootstrap-server localhost:9092
