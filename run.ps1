# Transport & Logistics - Local Development Launcher with PostgreSQL

Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host " Transport & Logistics Modular Monolith - Local PostgreSQL Runner" -ForegroundColor Cyan
Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host ""

# Ensure Docker PostgreSQL container is up if docker is present
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host "[*] Checking Docker PostgreSQL container..." -ForegroundColor Yellow
    try {
        docker compose up -d postgres
        Write-Host "[OK] PostgreSQL container is ready on port 5432." -ForegroundColor Green
    } catch {
        Write-Host "[INFO] Docker compose skipped or already active." -ForegroundColor Gray
    }
} else {
    Write-Host "[INFO] Docker not detected in PATH; using local PostgreSQL service on port 5432." -ForegroundColor Gray
}

Write-Host ""
Write-Host "[*] Launching Backend Spring Boot (Profile: postgres)..." -ForegroundColor Yellow
Start-Process -FilePath "wt" -ArgumentList "new-tab", "-d", "$PSScriptRoot", "--title", "Backend (Postgres)", "cmd", "/k", "mvn spring-boot:run" -ErrorAction SilentlyContinue `
    -ErrorVariable wtError

if ($wtError) {
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "title Backend (Postgres) && mvn spring-boot:run" -WorkingDirectory "$PSScriptRoot"
}

Write-Host "[*] Launching Frontend Vite / React..." -ForegroundColor Yellow
Start-Process -FilePath "wt" -ArgumentList "new-tab", "-d", "$PSScriptRoot\frontend", "--title", "Frontend (Port 5173)", "cmd", "/k", "npm run dev" -ErrorAction SilentlyContinue `
    -ErrorVariable wtError2

if ($wtError2) {
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "title Frontend (Port 5173) && cd frontend && npm run dev" -WorkingDirectory "$PSScriptRoot"
}

Write-Host ""
Write-Host "===================================================================" -ForegroundColor Green
Write-Host " Applications launched in separate windows!" -ForegroundColor Green
Write-Host "===================================================================" -ForegroundColor Green
Write-Host " Backend API:    http://localhost:8080/api"
Write-Host " Frontend UI:    http://localhost:5173/"
Write-Host " Swagger UI:     http://localhost:8080/api/swagger-ui.html"
Write-Host " Health Check:   http://localhost:8080/api/health"
Write-Host ""
Write-Host " Default Admin Login:"
Write-Host "   Username: admin"
Write-Host "   Password: AdminPass!2026"
Write-Host "===================================================================" -ForegroundColor Green
