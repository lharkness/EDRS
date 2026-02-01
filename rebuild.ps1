# EDRS Clean Rebuild Script (PowerShell)
# This script performs a complete clean rebuild of the EDRS system

param(
    [switch]$SkipImageRemoval,
    [switch]$SkipVolumeRemoval,
    [switch]$KeepCache,
    [switch]$Help
)

if ($Help) {
    Write-Host @"
EDRS Clean Rebuild Script

Usage: .\rebuild.ps1 [options]

Options:
    -SkipImageRemoval    Don't remove old Docker images (faster but less clean)
    -SkipVolumeRemoval   Don't remove volumes (preserves database data)
    -KeepCache           Use Docker build cache (faster but may miss changes)
    -Help                Show this help message

Examples:
    .\rebuild.ps1                           # Full clean rebuild
    .\rebuild.ps1 -SkipVolumeRemoval         # Keep database data
    .\rebuild.ps1 -KeepCache                 # Use cache (faster)
    .\rebuild.ps1 -SkipImageRemoval -KeepCache  # Quick rebuild
"@
    exit 0
}

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "EDRS Clean Rebuild Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Stop and remove containers
Write-Host "[1/6] Stopping and removing containers..." -ForegroundColor Yellow
if ($SkipVolumeRemoval) {
    docker compose down
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Warning: docker compose down failed, but continuing..." -ForegroundColor Yellow
    }
} else {
    docker compose down -v
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Warning: docker compose down -v failed, but continuing..." -ForegroundColor Yellow
    }
}
Write-Host "✓ Containers stopped and removed" -ForegroundColor Green
Write-Host ""

# Step 2: Remove old images (optional)
if (-not $SkipImageRemoval) {
    Write-Host "[2/6] Removing old EDRS images..." -ForegroundColor Yellow
    $images = docker images --format "{{.Repository}}:{{.Tag}}" | Select-String "edrs-"
    if ($images) {
        foreach ($image in $images) {
            Write-Host "  Removing $image..." -ForegroundColor Gray
            docker rmi -f $image 2>&1 | Out-Null
        }
        Write-Host "✓ Old images removed" -ForegroundColor Green
    } else {
        Write-Host "✓ No EDRS images found to remove" -ForegroundColor Green
    }
    Write-Host ""
} else {
    Write-Host "[2/6] Skipping image removal (--SkipImageRemoval)" -ForegroundColor Gray
    Write-Host ""
}

# Step 3: Clean Docker system (optional but recommended)
Write-Host "[3/6] Cleaning Docker system..." -ForegroundColor Yellow
docker system prune -f | Out-Null
Write-Host "✓ Docker system cleaned" -ForegroundColor Green
Write-Host ""

# Step 4: Rebuild images
Write-Host "[4/6] Rebuilding Docker images..." -ForegroundColor Yellow
if ($KeepCache) {
    Write-Host "  Using build cache (faster but may miss changes)" -ForegroundColor Gray
    docker compose build
} else {
    Write-Host "  Building without cache (slower but guarantees clean build)" -ForegroundColor Gray
    docker compose build --no-cache
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Images rebuilt successfully" -ForegroundColor Green
Write-Host ""

# Step 5: Start services
Write-Host "[5/6] Starting services..." -ForegroundColor Yellow
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Failed to start services!" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Services started" -ForegroundColor Green
Write-Host ""

# Step 6: Wait and verify
Write-Host "[6/6] Waiting for services to be healthy..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "Service Status:" -ForegroundColor Cyan
docker compose ps

Write-Host ""
Write-Host "Checking health endpoints..." -ForegroundColor Yellow

$services = @(
    @{Name="Reservation Service"; Port=8080},
    @{Name="Inventory Service"; Port=8081},
    @{Name="Persistence Service"; Port=8084}
)

$allHealthy = $true
foreach ($service in $services) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$($service.Port)/actuator/health" -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Host "  ✓ $($service.Name) (port $($service.Port))" -ForegroundColor Green
        } else {
            Write-Host "  ✗ $($service.Name) (port $($service.Port)) - Status: $($response.StatusCode)" -ForegroundColor Red
            $allHealthy = $false
        }
    } catch {
        Write-Host "  ✗ $($service.Name) (port $($service.Port)) - Not responding" -ForegroundColor Red
        $allHealthy = $false
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($allHealthy) {
    Write-Host "✓ Rebuild completed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Useful commands:" -ForegroundColor Cyan
    Write-Host "  docker compose logs -f              # View all logs" -ForegroundColor Gray
    Write-Host "  docker compose logs -f <service>     # View specific service logs" -ForegroundColor Gray
    Write-Host "  docker compose ps                     # Check service status" -ForegroundColor Gray
    Write-Host "  http://localhost:8090                # Swagger UI" -ForegroundColor Gray
    Write-Host "  http://localhost:9999                # Dozzle (log viewer)" -ForegroundColor Gray
} else {
    Write-Host "⚠ Rebuild completed but some services may not be healthy" -ForegroundColor Yellow
    Write-Host "  Check logs with: docker compose logs" -ForegroundColor Gray
}
Write-Host "========================================" -ForegroundColor Cyan
