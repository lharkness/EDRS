# Rebuild Scripts

This project includes automated scripts for clean rebuilds of the EDRS system.

## Quick Start

### Windows (PowerShell)
```powershell
.\rebuild.ps1
```

### Linux/Mac (Bash)
```bash
./rebuild.sh
```

### Using Make
```bash
make rebuild
```

## Script Options

### PowerShell (`rebuild.ps1`)
```powershell
# Full clean rebuild (recommended for production)
.\rebuild.ps1

# Keep database data
.\rebuild.ps1 -SkipVolumeRemoval

# Use build cache (faster)
.\rebuild.ps1 -KeepCache

# Quick rebuild (skip image removal, use cache)
.\rebuild.ps1 -SkipImageRemoval -KeepCache

# Show help
.\rebuild.ps1 -Help
```

### Bash (`rebuild.sh`)
```bash
# Full clean rebuild (recommended for production)
./rebuild.sh

# Keep database data
./rebuild.sh --skip-volume-removal

# Use build cache (faster)
./rebuild.sh --keep-cache

# Quick rebuild (skip image removal, use cache)
./rebuild.sh --skip-image-removal --keep-cache

# Show help
./rebuild.sh --help
```

## What the Scripts Do

1. **Stop and remove containers** - Stops all running services
2. **Remove volumes** (optional) - Deletes database and Kafka data
3. **Remove old images** (optional) - Cleans up old Docker images
4. **Clean Docker system** - Removes unused Docker resources
5. **Rebuild images** - Builds all services from scratch (with or without cache)
6. **Start services** - Starts all services in detached mode
7. **Verify health** - Checks that services are responding

## When to Use Each Option

### Full Clean Rebuild (Default)
```bash
./rebuild.sh
```
**Use when:**
- Deploying to production
- Schema changes were made
- You want to guarantee all changes are picked up
- You suspect caching issues

### Keep Volumes
```bash
./rebuild.sh --skip-volume-removal
```
**Use when:**
- You want to preserve database data
- You haven't made schema changes
- You're doing a quick rebuild during development

### Use Cache
```bash
./rebuild.sh --keep-cache
```
**Use when:**
- You only changed code (not dependencies)
- You want a faster rebuild
- You're iterating during development

### Quick Rebuild
```bash
./rebuild.sh --skip-image-removal --keep-cache
```
**Use when:**
- You want the fastest possible rebuild
- You're confident no major changes were made
- You're doing frequent rebuilds during development

## Makefile Targets

The Makefile also includes a `rebuild` target:

```bash
make rebuild
```

This performs a full clean rebuild (equivalent to `./rebuild.sh`).

## Manual Steps (if scripts don't work)

If you prefer to do it manually or the scripts fail:

```bash
# 1. Stop and remove everything
docker compose down -v

# 2. Remove old images
docker images | grep edrs | awk '{print $3}' | xargs docker rmi -f

# 3. Clean system
docker system prune -f

# 4. Rebuild without cache
docker compose build --no-cache

# 5. Start services
docker compose up -d

# 6. Check status
docker compose ps
docker compose logs -f
```

## Troubleshooting

### Script fails with permission errors
**Windows:** Run PowerShell as Administrator
**Linux/Mac:** Make script executable: `chmod +x rebuild.sh`

### Services don't start
Check logs: `docker compose logs`
Check ports: Ensure ports 8080, 8081, 8084, 5433, 9094 aren't in use

### Database schema not updated
Make sure you used `--skip-volume-removal` is NOT set, or manually remove the volume:
```bash
docker volume rm edrs_postgres-data
```

### Build takes too long
Use `--keep-cache` option for faster rebuilds during development

## Verification

After rebuild, verify everything is working:

```bash
# Check service status
docker compose ps

# Check health endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8084/actuator/health

# Or use the Makefile
make health
```

## Related Documentation

- See `REBUILD_GUIDE.md` for detailed manual steps
- See `QUICKSTART.md` for initial setup
- See `DOCKER.md` for Docker-specific information
