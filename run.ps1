# Transport & Logistics - PowerShell Application Runner
$ErrorActionPreference = "Stop"

Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host " Transport & Logistics Modular Monolith - PowerShell Runner" -ForegroundColor Cyan
Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Detect / Verify Java 21
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $candidates = @(
        "C:\Program Files\Java\jdk-21*",
        "C:\Program Files\Eclipse Adoptium\jdk-21*",
        "C:\Program Files\Microsoft\jdk-21*",
        "C:\Program Files\Amazon Corretto\jdk21*",
        "C:\Program Files\Zulu\zulu-21*",
        "C:\Program Files\BellSoft\LibericaJDK-21*"
    )
    foreach ($pattern in $candidates) {
        $found = Get-Item $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found -and (Test-Path "$($found.FullName)\bin\java.exe")) {
            $env:JAVA_HOME = $found.FullName
            $env:PATH = "$($found.FullName)\bin;$env:PATH"
            break
        }
    }
}

# 2. Check and start PostgreSQL via Docker Compose if needed
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host "[*] Ensuring PostgreSQL container is running..." -ForegroundColor Yellow
    docker compose up -d postgres | Out-Null
}

# 3. Resolve Maven command
$mvnCmd = if (Get-Command mvn -ErrorAction SilentlyContinue) { "mvn" } elseif (Test-Path ".\mvnw.cmd") { ".\mvnw.cmd" } else { "mvn" }

# 4. Start Backend and Frontend
Write-Host "[*] Launching Backend Spring Boot Application (Postgres)..." -ForegroundColor Green
Start-Process -FilePath "cmd.exe" -ArgumentList "/k title Backend - Spring Boot && $mvnCmd spring-boot:run -Dspring-boot.run.profiles=postgres"

Write-Host "[*] Launching Frontend Application (Vite / React)..." -ForegroundColor Green
Start-Process -FilePath "cmd.exe" -ArgumentList "/k title Frontend - Vite && cd frontend && npm run dev"

Write-Host ""
Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host " Applications started in separate windows!" -ForegroundColor Cyan
Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host " Backend API:    http://localhost:8080/api"
Write-Host " Frontend UI:    http://localhost:5173/"
Write-Host " Swagger UI:     http://localhost:8080/api/swagger-ui.html"
Write-Host " Health Check:   http://localhost:8080/api/health"
Write-Host ""
Write-Host " Default Admin Login:"
Write-Host "   Username: admin"
Write-Host "   Password: AdminPass!2026"
Write-Host "===================================================================" -ForegroundColor Cyan
