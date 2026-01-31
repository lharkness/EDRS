# Quick Start Guide

Get the entire EDRS system running with one command!

## Prerequisites

- Docker 20.10+ and Docker Compose 2.0+
- At least 4GB of available RAM
- Ports 8080-8090, 5432, 9092, 9093, 16686, 4317-4318 available

## One Command to Rule Them All

```bash
docker-compose up -d --build
```

That's it! This single command will start:
- ✅ All 5 microservices (Reservation, Inventory, Notification, Logging, Persistence)
- ✅ PostgreSQL database (with automatic schema creation)
- ✅ Kafka with KRaft (no Zookeeper required)
- ✅ Jaeger for distributed tracing
- ✅ OpenTelemetry Collector (scrapes HikariCP and application metrics)
- ✅ Kafka UI for topic management
- ✅ **Centralized Swagger UI** (all APIs in one place!)
- ✅ **Bulk CSV import** capability (via Inventory Service)
- ✅ **Bulk CSV import** capability (via Inventory Service)

## Access Everything

Once started, access all services at:

| Service | URL | Description |
|---------|-----|-------------|
| **Swagger UI** | http://localhost:8090 | **All APIs in one place!** |
| Reservation API | http://localhost:8080/swagger-ui.html | Reservation Service |
| Inventory API | http://localhost:8081/swagger-ui.html | Inventory Service (includes CSV bulk import) |
| Kafka UI | http://localhost:8089 | Manage Kafka topics |
| Jaeger UI | http://localhost:16686 | Distributed tracing |
| PostgreSQL | localhost:5432 | Database (user: postgres, password: postgres) |

## Verify Everything is Running

```bash
# Check all services
docker-compose ps

# View logs
docker-compose logs -f

# Check health
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8084/actuator/health
```

## Initialize Kafka Topics

After services start, initialize Kafka topics:

```bash
# Linux/Mac
./scripts/init-kafka-topics.sh

# Windows (PowerShell)
.\scripts\init-kafka-topics.ps1

# Or use Make
make init-topics
```

## Stop Everything

```bash
docker-compose down
```

## Next Steps

- Read [DOCKER.md](DOCKER.md) for detailed deployment instructions
- Read [readme.md](readme.md) for architecture and development setup
- Check out the [sequence diagrams](docs/sequence-diagrams.puml) to understand event flows

## Troubleshooting

If services don't start:

1. **Check ports are available**:
   ```bash
   netstat -an | grep -E "8080|8081|8082|8083|8084|5432|9092"
   ```

2. **Check Docker resources**:
   ```bash
   docker stats
   ```

3. **View service logs**:
   ```bash
   docker-compose logs reservation-service
   docker-compose logs kafka
   docker-compose logs postgres
   ```

4. **Rebuild if needed**:
   ```bash
   docker-compose up -d --build --force-recreate
   ```
