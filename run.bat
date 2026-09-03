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

:: Auto-detect JDK 21 if JAVA_HOME is not set or not pointing to Java 21
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" goto JAVA_FOUND
)

for %%D in (
    "C:\Program Files\Java\jdk-21*"
    "C:\Program Files\Eclipse Adoptium\jdk-21*"
    "C:\Program Files\Microsoft\jdk-21*"
    "C:\Program Files\Amazon Corretto\jdk21*"
    "C:\Program Files\Zulu\zulu-21*"
    "C:\Program Files\BellSoft\LibericaJDK-21*"
) do (
    if exist "%%~fD\bin\java.exe" (
        set "JAVA_HOME=%%~fD"
        set "PATH=%%~fD\bin;!PATH!"
        goto JAVA_FOUND
    )
)

:JAVA_FOUND
:: Resolve Maven command (mvn or mvnw.cmd)
where mvn >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set "MVN_CMD=mvn"
) else (
    if exist "mvnw.cmd" (
        set "MVN_CMD=mvnw.cmd"
    ) else (
        echo [!] Maven not found. Please install Maven or use mvnw.cmd.
        exit /b 1
    )
)

echo.
echo [*] Launching Backend Spring Boot Application (Profile: postgres)...
start "Backend - Transport & Logistics" cmd /k "title Backend - Spring Boot (Postgres) && %MVN_CMD% spring-boot:run -Dspring-boot.run.profiles=postgres"

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
