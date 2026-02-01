#!/bin/bash
# EDRS Clean Rebuild Script (Bash)
# This script performs a complete clean rebuild of the EDRS system

set -e  # Exit on error

SKIP_IMAGE_REMOVAL=false
SKIP_VOLUME_REMOVAL=false
KEEP_CACHE=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-image-removal)
            SKIP_IMAGE_REMOVAL=true
            shift
            ;;
        --skip-volume-removal)
            SKIP_VOLUME_REMOVAL=true
            shift
            ;;
        --keep-cache)
            KEEP_CACHE=true
            shift
            ;;
        --help|-h)
            echo "EDRS Clean Rebuild Script"
            echo ""
            echo "Usage: ./rebuild.sh [options]"
            echo ""
            echo "Options:"
            echo "  --skip-image-removal    Don't remove old Docker images (faster but less clean)"
            echo "  --skip-volume-removal   Don't remove volumes (preserves database data)"
            echo "  --keep-cache            Use Docker build cache (faster but may miss changes)"
            echo "  --help, -h              Show this help message"
            echo ""
            echo "Examples:"
            echo "  ./rebuild.sh                           # Full clean rebuild"
            echo "  ./rebuild.sh --skip-volume-removal     # Keep database data"
            echo "  ./rebuild.sh --keep-cache              # Use cache (faster)"
            echo "  ./rebuild.sh --skip-image-removal --keep-cache  # Quick rebuild"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo "========================================"
echo "EDRS Clean Rebuild Script"
echo "========================================"
echo ""

# Step 1: Stop and remove containers
echo "[1/6] Stopping and removing containers..."
if [ "$SKIP_VOLUME_REMOVAL" = true ]; then
    docker compose down || echo "Warning: docker compose down failed, but continuing..."
else
    docker compose down -v || echo "Warning: docker compose down -v failed, but continuing..."
fi
echo "✓ Containers stopped and removed"
echo ""

# Step 2: Remove old images (optional)
if [ "$SKIP_IMAGE_REMOVAL" = false ]; then
    echo "[2/6] Removing old EDRS images..."
    IMAGES=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep "edrs-" || true)
    if [ -n "$IMAGES" ]; then
        echo "$IMAGES" | while read -r image; do
            echo "  Removing $image..."
            docker rmi -f "$image" 2>/dev/null || true
        done
        echo "✓ Old images removed"
    else
        echo "✓ No EDRS images found to remove"
    fi
    echo ""
else
    echo "[2/6] Skipping image removal (--skip-image-removal)"
    echo ""
fi

# Step 3: Clean Docker system (optional but recommended)
echo "[3/6] Cleaning Docker system..."
docker system prune -f > /dev/null
echo "✓ Docker system cleaned"
echo ""

# Step 4: Rebuild images
echo "[4/6] Rebuilding Docker images..."
if [ "$KEEP_CACHE" = true ]; then
    echo "  Using build cache (faster but may miss changes)"
    docker compose build
else
    echo "  Building without cache (slower but guarantees clean build)"
    docker compose build --no-cache
fi

if [ $? -ne 0 ]; then
    echo "✗ Build failed!"
    exit 1
fi
echo "✓ Images rebuilt successfully"
echo ""

# Step 5: Start services
echo "[5/6] Starting services..."
docker compose up -d
if [ $? -ne 0 ]; then
    echo "✗ Failed to start services!"
    exit 1
fi
echo "✓ Services started"
echo ""

# Step 6: Wait and verify
echo "[6/6] Waiting for services to be healthy..."
sleep 10

echo ""
echo "Service Status:"
docker compose ps

echo ""
echo "Checking health endpoints..."

check_health() {
    local name=$1
    local port=$2
    if curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
        echo "  ✓ $name (port $port)"
        return 0
    else
        echo "  ✗ $name (port $port) - Not responding"
        return 1
    fi
}

ALL_HEALTHY=true
check_health "Reservation Service" 8080 || ALL_HEALTHY=false
check_health "Inventory Service" 8081 || ALL_HEALTHY=false
check_health "Persistence Service" 8084 || ALL_HEALTHY=false

echo ""
echo "========================================"
if [ "$ALL_HEALTHY" = true ]; then
    echo "✓ Rebuild completed successfully!"
    echo ""
    echo "Useful commands:"
    echo "  docker compose logs -f              # View all logs"
    echo "  docker compose logs -f <service>     # View specific service logs"
    echo "  docker compose ps                     # Check service status"
    echo "  http://localhost:8090                # Swagger UI"
    echo "  http://localhost:9999                # Dozzle (log viewer)"
else
    echo "⚠ Rebuild completed but some services may not be healthy"
    echo "  Check logs with: docker compose logs"
fi
echo "========================================"
