# Clean Rebuild and Redeploy Guide

This guide ensures a complete clean rebuild of the EDRS system, guaranteeing all code changes are picked up.

## Prerequisites

- Docker and Docker Compose installed
- Access to the project directory
- Sufficient disk space (old images will be removed)

## Step-by-Step Clean Rebuild Process

### Step 1: Stop All Running Containers

```bash
docker compose down
```

This stops and removes all containers, but keeps volumes and images.

### Step 2: Remove All Containers, Networks, and Volumes

```bash
docker compose down -v
```

The `-v` flag removes volumes as well. This ensures:
- No stale data in PostgreSQL
- No stale Kafka metadata
- Fresh database schema initialization

**Note:** This will delete all data! If you need to preserve data, skip this step or backup volumes first.

### Step 3: Remove Old Images (Optional but Recommended)

To ensure you're building from scratch, remove the old service images:

```bash
docker images | grep edrs
```

Then remove specific images:

```bash
docker rmi edrs-reservation-service edrs-inventory-service edrs-persistence-service edrs-notification-service edrs-logging-service
```

Or remove all EDRS images at once:

```bash
docker images | grep edrs | awk '{print $3}' | xargs docker rmi -f
```

### Step 4: Clean Build (No Cache)

Rebuild all services from scratch without using cache:

```bash
docker compose build --no-cache
```

The `--no-cache` flag ensures:
- All Maven dependencies are re-downloaded
- All code changes are compiled fresh
- No stale build artifacts are used

**Note:** This takes longer but guarantees a clean build.

### Step 5: Start the System

Start all services:

```bash
docker compose up -d
```

The `-d` flag runs in detached mode.

### Step 6: Monitor Startup

Watch the logs to ensure services start correctly:

```bash
docker compose logs -f
```

Or watch specific services:

```bash
docker compose logs -f reservation-service persistence-service inventory-service
```

### Step 7: Verify Services Are Healthy

Check service status:

```bash
docker compose ps
```

All services should show "Up" status. Wait for health checks to pass (especially PostgreSQL and Kafka).

### Step 8: Verify Database Schema

If you made schema changes, verify the database:

```bash
docker exec edrs-postgres psql -U postgres -d edrs -c "\d reservation_items"
```

This should show the `quantity` column if schema changes were applied.

### Step 9: Test the API

Verify the services are responding:

```bash
# Test reservation service
curl http://localhost:8080/actuator/health

# Test inventory service
curl http://localhost:8081/actuator/health

# Test persistence service
curl http://localhost:8082/actuator/health
```

## Quick One-Liner (Complete Clean Rebuild)

For a complete clean rebuild in one command:

```bash
docker compose down -v && docker compose build --no-cache && docker compose up -d
```

## Troubleshooting

### If Services Fail to Start

1. Check logs: `docker compose logs <service-name>`
2. Verify ports aren't in use: `netstat -an | grep 8080` (or use `lsof` on Linux/Mac)
3. Check disk space: `docker system df`
4. Clean up Docker system: `docker system prune -a` (removes all unused images, containers, networks)

### If Database Schema Changes Don't Apply

1. Ensure `schema-init.sql` is updated
2. Remove the postgres volume: `docker volume rm edrs_postgres-data`
3. Rebuild and restart

### If Kafka Has Issues

1. Remove Kafka volume: `docker volume rm edrs_kafka-data`
2. Rebuild and restart (Kafka will reformat metadata)

## Partial Rebuild (Faster, Less Guaranteed)

If you only changed one service and want a faster rebuild:

```bash
# Stop specific service
docker compose stop reservation-service

# Rebuild specific service (with cache)
docker compose build reservation-service

# Start specific service
docker compose up -d reservation-service
```

**Warning:** This may use cached layers and might not pick up all changes.

## Best Practices

1. **Always use `--no-cache` for production deployments** to ensure all changes are included
2. **Remove volumes when schema changes** to ensure clean database initialization
3. **Check logs after startup** to catch any initialization errors early
4. **Verify health endpoints** before considering deployment complete
5. **Keep a backup** of important data before `docker compose down -v`

## Verification Checklist

- [ ] All containers are running (`docker compose ps`)
- [ ] Health endpoints return 200 OK
- [ ] Database schema is correct (if changed)
- [ ] Services can communicate (check logs for connection errors)
- [ ] API endpoints respond correctly
- [ ] No errors in logs (`docker compose logs`)
