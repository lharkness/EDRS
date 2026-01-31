# HikariCP Metrics with OpenTelemetry

## Overview

HikariCP automatically exposes metrics through Spring Boot Actuator and Micrometer. These metrics can be consumed by OpenTelemetry in several ways.

## Available HikariCP Metrics

When using Spring Boot with HikariCP, the following metrics are automatically registered:

- `hikari.connections.active` - Number of active connections
- `hikari.connections.idle` - Number of idle connections  
- `hikari.connections.pending` - Number of pending connection requests
- `hikari.connections.timeout` - Number of connection timeouts
- `hikari.connections.creation` - Time taken to create connections
- `hikari.connections.acquire` - Time taken to acquire connections from pool
- `hikari.connections.usage` - Duration of connection usage

## Accessing Metrics

### Via Prometheus Endpoint

Metrics are available at:
```
GET /actuator/prometheus
```

Example HikariCP metrics in Prometheus format:
```
hikari_connections_active{pool="EDRS-Persistence-Pool"} 5.0
hikari_connections_idle{pool="EDRS-Persistence-Pool"} 3.0
hikari_connections_pending{pool="EDRS-Persistence-Pool"} 0.0
```

### Via Metrics Endpoint

List all available metrics:
```
GET /actuator/metrics
```

Get specific metric:
```
GET /actuator/metrics/hikari.connections.active
```

## Integration with OpenTelemetry

### Option 1: OpenTelemetry Collector (Recommended)

Use the OpenTelemetry Collector to scrape Prometheus metrics and forward to your observability backend:

1. **Configure OpenTelemetry Collector** (`otel-collector-config.yaml`):
```yaml
receivers:
  prometheus:
    config:
      scrape_configs:
        - job_name: 'persistence-service'
          scrape_interval: 10s
          metrics_path: '/actuator/prometheus'
          static_configs:
            - targets: ['localhost:8084']

exporters:
  otlp:
    endpoint: localhost:4317
    tls:
      insecure: true

service:
  pipelines:
    metrics:
      receivers: [prometheus]
      exporters: [otlp]
```

2. **Run OpenTelemetry Collector**:
```bash
docker run -v $(pwd)/otel-collector-config.yaml:/etc/otel-collector-config.yaml \
  otel/opentelemetry-collector:latest \
  --config=/etc/otel-collector-config.yaml
```

### Option 2: Direct OTLP Export

Configure Micrometer to export directly to OpenTelemetry via OTLP:

1. **Enable OTLP export** in `application.yml`:
```yaml
management:
  metrics:
    export:
      otlp:
        enabled: true
        url: http://localhost:4318/v1/metrics
        step: 10s
```

2. **Set environment variable**:
```bash
export OTEL_METRICS_EXPORT_ENABLED=true
export OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=http://localhost:4318/v1/metrics
```

### Option 3: OpenTelemetry Java Agent

Use the OpenTelemetry Java agent which automatically instruments HikariCP:

1. **Download the agent**:
```bash
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.32.0/opentelemetry-javaagent.jar
```

2. **Run with agent**:
```bash
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=persistence-service \
  -Dotel.exporter.otlp.endpoint=http://localhost:4317 \
  -jar persistence-service.jar
```

The agent automatically instruments HikariCP and exports metrics to OpenTelemetry.

## Viewing Metrics

### In Jaeger

If using Jaeger with OpenTelemetry:
1. Metrics will appear in Jaeger UI under the service name
2. Navigate to the service and view metrics dashboard

### In Prometheus

If scraping Prometheus:
1. Query HikariCP metrics:
   ```promql
   hikari_connections_active{pool="EDRS-Persistence-Pool"}
   ```

2. Create alerts for connection pool issues:
   ```promql
   hikari_connections_pending > 5  # Alert if too many pending requests
   ```

### In Grafana

Create dashboards using Prometheus queries or OpenTelemetry metrics:
- Connection pool utilization
- Connection wait times
- Connection creation/usage durations

## Best Practices

1. **Monitor Connection Pool Health**:
   - Alert when `hikari.connections.pending` > 0 for extended periods
   - Alert when `hikari.connections.active` approaches `maximum-pool-size`
   - Monitor `hikari.connections.timeout` for connection issues

2. **Performance Tuning**:
   - Use `hikari.connections.acquire` to identify slow connection acquisition
   - Use `hikari.connections.usage` to understand connection usage patterns
   - Adjust pool size based on active connection trends

3. **Troubleshooting**:
   - High `pending` values indicate pool exhaustion
   - Frequent `timeout` values suggest network or database issues
   - Long `creation` times indicate database connection problems

## Configuration

HikariCP metrics are automatically enabled when:
- Spring Boot Actuator is on the classpath
- `management.endpoints.web.exposure.include` includes `metrics` or `prometheus`

No additional configuration is needed for basic metrics exposure.
