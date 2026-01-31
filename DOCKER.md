# Docker Deployment Guide

This guide explains how to deploy the EDRS system using Docker and Docker Compose.

## Prerequisites

- Docker 20.10+ and Docker Compose 2.0+
- At least 4GB of available RAM
- Ports 8080-8090, 5433 (PostgreSQL), 9094 (Kafka), 9093, 16686, 4317-4318 available

## Quick Start

### 1. Build and Start All Services

```bash
# Build and start all services
docker-compose up -d --build

# Initialize Kafka topics (required)
# On Linux/Mac:
./scripts/init-kafka-topics.sh

# On Windows (PowerShell):
.\scripts\init-kafka-topics.ps1

# Or use Make:
make up  # This automatically initializes topics

# View logs
docker-compose logs -f

# Check service status
docker-compose ps
```

### 2. Verify Services

```bash
# Check all services are healthy
docker-compose ps

# Test endpoints
curl http://localhost:8080/actuator/health  # Reservation Service
curl http://localhost:8081/actuator/health  # Inventory Service
curl http://localhost:8082/actuator/health  # Notification Service
curl http://localhost:8083/actuator/health  # Logging Service
curl http://localhost:8084/actuator/health  # Persistence Service
```

### 3. Access Services

- **Centralized Swagger UI** (All APIs): http://localhost:8090
- **Reservation Service API**: http://localhost:8080/swagger-ui.html
- **Inventory Service API**: http://localhost:8081/swagger-ui.html
  - Includes **CSV bulk import** endpoint: `POST /api/inventory/receive/bulk`
  - See [BULK_IMPORT.md](docs/BULK_IMPORT.md) for details
- **Kafka UI**: http://localhost:8089
- **Jaeger UI**: http://localhost:16686
- **PostgreSQL**: localhost:5433 (user: postgres, password: postgres, db: edrs)
   - Note: Default port is 5433 to avoid conflicts with host PostgreSQL. Override with `POSTGRES_PORT` environment variable.

## Service Architecture

The Docker Compose setup includes:

1. **Database**: PostgreSQL 15 (with automatic schema initialization)
2. **Message Broker**: Kafka 7.5.0 with KRaft (no Zookeeper required)
3. **Services**: All 5 EDRS microservices
4. **Observability**: Jaeger + OpenTelemetry Collector (scrapes HikariCP metrics)
5. **Management**: Kafka UI + Centralized Swagger UI
6. **Features**: Bulk CSV inventory import available via Inventory Service API

## Building Individual Services

```bash
# Build a specific service
docker-compose build reservation-service

# Rebuild without cache
docker-compose build --no-cache reservation-service
```

## Environment Variables

### Database Configuration

```bash
export POSTGRES_DB=edrs
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=your_password
export POSTGRES_PORT=5433  # Default is 5433 to avoid conflicts with host PostgreSQL
```

### Kafka Configuration

```bash
export KAFKA_PORT=9094  # Default is 9094 to avoid conflicts with host Kafka
```

### Service Configuration

Services can be configured via environment variables in `docker-compose.yml`:

- `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker address (default: `kafka:29092`)
- `DATABASE_URL`: PostgreSQL connection URL
- `DATABASE_USERNAME`: PostgreSQL username
- `DATABASE_PASSWORD`: PostgreSQL password
- `JAEGER_ENDPOINT`: Jaeger endpoint for tracing

## Data Persistence

Docker volumes are used for data persistence:

- `postgres-data`: PostgreSQL data
- `kafka-data`: Kafka logs and data

To remove all data:

```bash
docker-compose down -v
```

## Health Checks

All services include health checks. Check status:

```bash
# View health status
docker-compose ps

# Check specific service
docker inspect edrs-reservation-service | grep -A 10 Health
```

## Logs

### View All Logs

```bash
docker-compose logs -f
```

### View Specific Service Logs

```bash
docker-compose logs -f reservation-service
docker-compose logs -f kafka
docker-compose logs -f postgres
```

### View Last 100 Lines

```bash
docker-compose logs --tail=100 reservation-service
```

## Troubleshooting

### Services Won't Start

1. **Check port conflicts**:
   ```bash
   # Check if ports are in use
   netstat -an | grep -E "8080|8081|8082|8083|8084|5433|9094"
   ```

2. **Check Docker resources**:
   ```bash
   docker stats
   ```

3. **View service logs**:
   ```bash
   docker-compose logs service-name
   ```

### Kafka Connection Issues

If services can't connect to Kafka:

1. **Wait for Kafka to be healthy**:
   ```bash
   docker-compose ps kafka
   # Should show "healthy" status
   ```

2. **Check Kafka logs**:
   ```bash
   docker-compose logs kafka
   ```

3. **Verify Kafka is accessible**:
   ```bash
   docker exec -it edrs-kafka kafka-broker-api-versions --bootstrap-server localhost:29092
   ```

### Database Connection Issues

If persistence service can't connect to PostgreSQL:

1. **Wait for PostgreSQL to be ready**:
   ```bash
   docker-compose ps postgres
   # Should show "healthy" status
   ```

2. **Check PostgreSQL logs**:
   ```bash
   docker-compose logs postgres
   ```

3. **Test connection**:
   ```bash
   docker exec -it edrs-postgres psql -U postgres -d edrs -c "SELECT 1;"
   ```

### Service Build Failures

If services fail to build:

1. **Clean build**:
   ```bash
   docker-compose build --no-cache
   ```

2. **Check Maven dependencies**:
   ```bash
   # Build locally first
   mvn clean install -DskipTests
   ```

3. **Check Dockerfile paths**:
   - Ensure Dockerfiles are in correct locations
   - Verify build context in docker-compose.yml

## Development Workflow

### Hot Reload (Not Included)

For development with hot reload, mount source code:

```yaml
# Add to service in docker-compose.yml
volumes:
  - ./reservation-service/src:/app/src
```

Then use Spring Boot DevTools or similar.

### Running Tests

```bash
# Run tests in container
docker-compose exec reservation-service mvn test

# Or run locally
mvn test
```

## Production Considerations

### Security

1. **Change default passwords**:
   ```bash
   export POSTGRES_PASSWORD=strong_password
   ```

2. **Use secrets management**:
   - Use Docker secrets
   - Or external secret management (HashiCorp Vault, AWS Secrets Manager)

3. **Network isolation**:
   - Use custom networks
   - Restrict port exposure

### Performance

1. **Resource limits**:
   ```yaml
   services:
     reservation-service:
       deploy:
         resources:
           limits:
             cpus: '1'
             memory: 512M
   ```

2. **Database tuning**:
   - Adjust PostgreSQL configuration
   - Use connection pooling (already configured)

3. **Kafka tuning**:
   - Adjust partition count
   - Configure replication factor for production

### Monitoring

1. **Enable metrics export**:
   - Prometheus endpoints are already exposed
   - Configure scraping in your monitoring system

2. **Log aggregation**:
   - Use Docker logging drivers
   - Integrate with ELK, Loki, etc.

### High Availability

1. **Database**:
   - Use managed PostgreSQL service
   - Set up replication

2. **Kafka**:
   - Use Kafka cluster (multiple brokers)
   - Configure replication factor > 1

3. **Services**:
   - Run multiple instances
   - Use load balancer

## Stopping Services

```bash
# Stop all services
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop and remove containers + volumes (⚠️ deletes data)
docker-compose down -v
```

## Updating Services

```bash
# Pull latest code
git pull

# Rebuild and restart
docker-compose up -d --build

# Or restart specific service
docker-compose up -d --build reservation-service
```

## Cleanup

```bash
# Remove all containers, networks, and volumes
docker-compose down -v

# Remove unused images
docker image prune -a

# Full cleanup (⚠️ removes all unused Docker resources)
docker system prune -a --volumes
```

## Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [Kafka Docker Guide](https://kafka.apache.org/documentation/#docker)
- [PostgreSQL Docker Guide](https://hub.docker.com/_/postgres)
