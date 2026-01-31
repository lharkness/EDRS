# OpenTelemetry Collector Setup

This directory contains the configuration for the OpenTelemetry Collector, which scrapes Prometheus metrics (including HikariCP metrics) from Spring Boot Actuator endpoints and forwards them to OpenTelemetry backends.

## Overview

The OpenTelemetry Collector:
1. **Scrapes** Prometheus metrics from all EDRS services via `/actuator/prometheus` endpoints
2. **Processes** the metrics (batching, resource attribution, transformation)
3. **Exports** metrics to OpenTelemetry backends (Jaeger, OTLP endpoints, etc.)

## Quick Start

### Option 1: Docker Compose (Recommended)

1. **Start the collector and Jaeger**:
   ```bash
   cd otel-collector
   docker-compose up -d
   ```

2. **Verify the collector is running**:
   ```bash
   docker ps | grep otel-collector
   ```

3. **Check collector logs**:
   ```bash
   docker logs -f otel-collector
   ```

4. **Access Jaeger UI**: http://localhost:16686

### Option 2: Standalone Docker

1. **Start the collector** (using contrib image for Prometheus receiver):
   ```bash
   docker run -d --name otel-collector \
     -p 4317:4317 \
     -p 4318:4318 \
     -p 8889:8889 \
     -v $(pwd)/otel-collector/otel-collector-config.yaml:/etc/otel-collector-config.yaml \
     -e OTEL_EXPORTER_OTLP_ENDPOINT=localhost:4317 \
     otel/opentelemetry-collector-contrib:latest \
     --config=/etc/otel-collector-config.yaml
   ```
   
   **Note**: Use `otel/opentelemetry-collector-contrib` (not the base image) as it includes the Prometheus receiver.

### Option 3: Local Installation

1. **Download OpenTelemetry Collector**:
   ```bash
   # Linux/Mac
   wget https://github.com/open-telemetry/opentelemetry-collector-releases/releases/download/v0.92.0/otelcol_0.92.0_linux_amd64.tar.gz
   tar -xzf otelcol_0.92.0_linux_amd64.tar.gz
   
   # Or use Homebrew (Mac)
   brew install opentelemetry-collector
   ```

2. **Run the collector**:
   ```bash
   ./otelcol --config=otel-collector/otel-collector-config.yaml
   ```

## Configuration

### Service Endpoints

The collector is configured to scrape metrics from:
- **Persistence Service**: `host.docker.internal:8084/actuator/prometheus` (default)
- **Reservation Service**: `host.docker.internal:8080/actuator/prometheus` (default)
- **Inventory Service**: `host.docker.internal:8081/actuator/prometheus` (default)
- **Notification Service**: `host.docker.internal:8082/actuator/prometheus` (default)
- **Logging Service**: `host.docker.internal:8083/actuator/prometheus` (default)

**Note**: The default `host.docker.internal` works on Docker Desktop (Windows/Mac). For Linux or custom setups, you can override these via environment variables in `docker-compose.yml`.

### Environment Variables

You can customize the collector behavior via environment variables:

```bash
# OTLP Export Endpoint (default: localhost:4317)
export OTEL_EXPORTER_OTLP_ENDPOINT=localhost:4317

# Deployment Environment (default: development)
export DEPLOYMENT_ENVIRONMENT=production

# Service Namespace (default: edrs)
export SERVICE_NAMESPACE=edrs

# Log Level (default: info)
export OTEL_LOG_LEVEL=debug
```

### Docker Compose Environment

Edit `docker-compose.yml` or use a `.env` file:

```bash
# .env file
OTEL_EXPORTER_OTLP_ENDPOINT=jaeger:4317
DEPLOYMENT_ENVIRONMENT=development
SERVICE_NAMESPACE=edrs
OTEL_LOG_LEVEL=info
```

## Verifying Metrics Collection

### 1. Check Collector Health

```bash
curl http://localhost:8888/metrics
```

### 2. Check Prometheus Export (Optional)

```bash
curl http://localhost:8889/metrics
```

### 3. Verify Service Metrics

Ensure your services are exposing metrics:
```bash
# Check persistence service
curl http://localhost:8084/actuator/prometheus | grep hikari

# Should see metrics like:
# hikari_connections_active{pool="EDRS-Persistence-Pool"} 5.0
# hikari_connections_idle{pool="EDRS-Persistence-Pool"} 3.0
```

### 4. View Metrics in Jaeger

1. Open Jaeger UI: http://localhost:16686
2. Select your service (e.g., `persistence-service`)
3. Navigate to the metrics view
4. Look for HikariCP metrics

## HikariCP Metrics Available

The collector will automatically scrape and forward these HikariCP metrics:

- `hikari.connections.active` - Active connections
- `hikari.connections.idle` - Idle connections
- `hikari.connections.pending` - Pending connection requests
- `hikari.connections.timeout` - Connection timeouts
- `hikari.connections.creation` - Connection creation time
- `hikari.connections.acquire` - Connection acquisition time
- `hikari.connections.usage` - Connection usage duration

## Troubleshooting

### Collector Not Scraping Metrics

1. **Check service endpoints are accessible**:
   ```bash
   curl http://localhost:8084/actuator/prometheus
   ```

2. **Check collector logs**:
   ```bash
   docker logs otel-collector
   ```

3. **Verify network connectivity** (for Docker):
   - On Windows/Mac, use `host.docker.internal` instead of `localhost`
   - On Linux, use `172.17.0.1` or the host IP

### Metrics Not Appearing in Jaeger

1. **Verify OTLP endpoint**:
   ```bash
   # Check if Jaeger is listening
   docker logs jaeger | grep OTLP
   ```

2. **Check collector exporter logs**:
   ```bash
   docker logs otel-collector | grep exporter
   ```

3. **Verify service names**:
   - Ensure service names match in collector config and application.yml

### Docker Network Issues

If services can't reach each other:

1. **On Linux**: The default config uses `host.docker.internal` which works on Docker Desktop (Windows/Mac). For Linux:
   - Edit `otel-collector-config.yaml` and replace `host.docker.internal` with `172.17.0.1` or your host IP
   - Or use `localhost` if running collector on host
   - Or use Docker network and service names

2. **Use Docker network**:
   ```bash
   docker network create edrs-network
   docker network connect edrs-network <container-name>
   ```

3. **Update host references**:
   - Change `host.docker.internal` to actual service container names
   - Or use Docker Compose service names

## Production Considerations

### 1. Security

- Use TLS for OTLP export:
  ```yaml
  exporters:
    otlp:
      tls:
        insecure: false
        cert_file: /path/to/cert.pem
        key_file: /path/to/key.pem
  ```

- Secure Prometheus scraping:
  - Use authentication headers
  - Use TLS for service endpoints

### 2. Performance

- Adjust batch processor settings:
  ```yaml
  processors:
    batch:
      timeout: 5s
      send_batch_size: 2048
  ```

- Scale collector horizontally for high throughput

### 3. High Availability

- Run multiple collector instances
- Use load balancer for service endpoints
- Configure collector clustering

### 4. Resource Limits

Set appropriate resource limits in Docker:
```yaml
services:
  otel-collector:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
```

## Advanced Configuration

### Custom Processors

Add custom processors for filtering, transformation, etc.:

```yaml
processors:
  filter:
    metrics:
      exclude:
        match_type: regexp
        metric_names:
          - ".*_total$"  # Exclude counter totals
```

### Multiple Exporters

Export to multiple backends:

```yaml
exporters:
  otlp/jaeger:
    endpoint: jaeger:4317
  otlp/prometheus:
    endpoint: prometheus:4317
  logging:
    loglevel: debug
```

## References

- [OpenTelemetry Collector Documentation](https://opentelemetry.io/docs/collector/)
- [Prometheus Receiver](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/prometheusreceiver)
- [OTLP Exporter](https://github.com/open-telemetry/opentelemetry-collector/tree/main/exporter/otlpexporter)
