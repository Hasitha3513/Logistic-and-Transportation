@echo off
setlocal enabledelayedexpansion

echo ===================================================================
echo  Transport ^& Logistics Modular Monolith - Local PostgreSQL Runner
echo ===================================================================
echo.

:: Ensure Docker PostgreSQL is running if docker is available
where docker >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [*] Checking Docker PostgreSQL container...
    docker compose up -d postgres 2>nul
    if %ERRORLEVEL% equ 0 (
        echo [OK] PostgreSQL container is ready on port 5432.
    ) else (
        echo [INFO] Docker compose command skipped or already running.
    )
) else (
    echo [INFO] Docker not found in PATH; using local PostgreSQL service on port 5432.
)

if exist "C:\Program Files\Java\jdk-21" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    set "PATH=C:\Program Files\Java\jdk-21\bin;%PATH%"
)

echo.
echo [*] Launching Backend Spring Boot Application (Profile: postgres)...
start "Backend - Transport & Logistics" cmd /k "title Backend - Spring Boot (Postgres) && set JAVA_HOME=C:\Program Files\Java\jdk-21&& set PATH=C:\Program Files\Java\jdk-21\bin;%%PATH%%&& mvn spring-boot:run -Dspring-boot.run.profiles=postgres"

echo [*] Launching Frontend Application (Vite / React)...
start "Frontend - Transport & Logistics" cmd /k "title Frontend - Vite (Port 5173) && cd frontend && npm run dev"

echo.
echo ===================================================================
echo  Applications started in separate windows!
echo ===================================================================
echo  Backend API:    http://localhost:8080/api
echo  Frontend UI:    http://localhost:5173/
echo  Swagger UI:     http://localhost:8080/api/swagger-ui.html
echo  Health Check:   http://localhost:8080/api/health
echo.
echo  Default Admin Login:
echo    Username: admin
echo    Password: AdminPass!2026
echo ===================================================================
echo.
pause
