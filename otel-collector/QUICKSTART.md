# Quick Start Guide - OpenTelemetry Collector

This guide will get you up and running with the OpenTelemetry Collector in under 5 minutes.

## Prerequisites

- Docker and Docker Compose installed
- EDRS services running (on localhost ports 8080-8084)

## Step 1: Start the Collector

```bash
cd otel-collector
docker-compose up -d
```

## Step 2: Verify It's Running

```bash
# Check containers are up
docker ps | grep -E "otel-collector|jaeger"

# Check collector logs
docker logs otel-collector
```

You should see logs indicating the collector is running and scraping metrics.

## Step 3: Access Jaeger UI

Open your browser and navigate to:
```
http://localhost:16686
```

## Step 4: Verify Metrics Collection

### Check HikariCP Metrics

```bash
# Check if persistence service is exposing metrics
curl http://localhost:8084/actuator/prometheus | grep hikari
```

You should see metrics like:
```
hikari_connections_active{pool="EDRS-Persistence-Pool"} 5.0
hikari_connections_idle{pool="EDRS-Persistence-Pool"} 3.0
```

### Check Collector is Scraping

```bash
# Check collector's own metrics
curl http://localhost:8888/metrics | grep prometheus
```

## Step 5: View Metrics in Jaeger

1. Open Jaeger UI: http://localhost:16686
2. Select a service (e.g., `persistence-service`)
3. Navigate to the metrics view
4. Look for HikariCP connection pool metrics

## Troubleshooting

### Services Not Reachable

If the collector can't reach your services:

**On Windows/Mac (Docker Desktop):**
- The default `host.docker.internal` should work
- If not, find your host IP: `ipconfig` (Windows) or `ifconfig` (Mac)
- Update `docker-compose.yml` environment variables

**On Linux:**
- Use `172.17.0.1` or your host IP instead of `host.docker.internal`
- Or run services in the same Docker network

### No Metrics in Jaeger

1. **Check service endpoints**:
   ```bash
   curl http://localhost:8084/actuator/prometheus
   ```

2. **Check collector logs**:
   ```bash
   docker logs -f otel-collector
   ```

3. **Verify OTLP export**:
   ```bash
   docker logs otel-collector | grep exporter
   ```

## Next Steps

- See `README.md` for detailed configuration options
- See `docs/HIKARICP_METRICS.md` for HikariCP metrics documentation
- Customize `otel-collector-config.yaml` for your environment

## Stop the Collector

```bash
docker-compose down
```
