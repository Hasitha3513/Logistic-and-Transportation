#!/usr/bin/env bash
set -e

echo "==================================================================="
echo " Transport & Logistics Modular Monolith - Application Runner"
echo "==================================================================="
echo ""

# Export JDK 21 if present
if [ -d "/usr/lib/jvm/jdk-21.0.12.1-oracle-x64" ]; then
    export JAVA_HOME="/usr/lib/jvm/jdk-21.0.12.1-oracle-x64"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

if [ -d "/usr/lib/chatgpt/resources/cua_node/bin" ]; then
    export PATH="/usr/lib/chatgpt/resources/cua_node/bin:$PATH"
fi

# Detect PostgreSQL availability on port 5432
if nc -z localhost 5432 2>/dev/null; then
    echo "[*] Detected running PostgreSQL on port 5432. Using 'postgres' profile..."
    PROFILE="postgres"
else
    echo "[*] PostgreSQL not detected on port 5432. Using embedded 'h2' profile with complete sample dataset..."
    PROFILE="h2"
fi

echo "[*] Launching Backend Spring Boot Application (Profile: ${PROFILE})..."
mvn spring-boot:run -Dspring-boot.run.profiles=${PROFILE} -Dspring-boot.run.jvmArguments="-Dapp.dev.identity-bootstrap.enabled=true -Dapp.dev.identity-bootstrap.username=admin -Dapp.dev.identity-bootstrap.password=AdminPassword123! -Dapp.dev.sample-data.enabled=true" &
BACKEND_PID=$!

echo "[*] Launching Frontend Application (Vite / React)..."
(cd frontend && npm run dev -- --host 0.0.0.0) &
FRONTEND_PID=$!

cleanup() {
    echo ""
    echo "[*] Stopping applications..."
    kill $BACKEND_PID $FRONTEND_PID 2>/dev/null || true
}
trap cleanup EXIT INT TERM

echo ""
echo "==================================================================="
echo " Applications running!"
echo "==================================================================="
echo " Backend API:    http://localhost:8080/api"
echo " Frontend UI:    http://localhost:5173/"
echo " Swagger UI:     http://localhost:8080/api/swagger-ui.html"
echo " Health Check:   http://localhost:8080/api/health"
echo ""
echo " Default Admin Login:"
echo "   Username: admin"
echo "   Password: AdminPassword123!"
echo "==================================================================="
echo ""

wait
