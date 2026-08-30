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

for port in 8080 5173; do
    if nc -z localhost "$port" 2>/dev/null; then
        echo "[!] Port ${port} is already in use. Stop the existing application before running this script again."
        exit 1
    fi
done

# Load the same database settings used by Docker Compose.
if [ -f ".env" ]; then
    set -a
    # shellcheck disable=SC1091
    . ./.env
    set +a
fi

export DB_USERNAME="${DB_USERNAME:-${POSTGRES_USER:-transport_app}}"
export DB_PASSWORD="${DB_PASSWORD:-${POSTGRES_PASSWORD:-transport_app_secret}}"
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:${POSTGRES_PORT:-5432}/${POSTGRES_DB:-transport_logistics}}"

# Ensure PostgreSQL container is running if docker is present and port is not open
if ! nc -z localhost "${POSTGRES_PORT:-5432}" 2>/dev/null; then
    if command -v docker >/dev/null 2>&1; then
        echo "[*] PostgreSQL is not running. Starting PostgreSQL container via Docker Compose..."
        docker compose up -d postgres
        sleep 3
    else
        echo "[!] PostgreSQL is not reachable at localhost:${POSTGRES_PORT:-5432} and Docker is not available."
        echo "    Please start PostgreSQL before running the application."
        exit 1
    fi
fi

echo "[*] Using PostgreSQL datasource at ${DB_URL}..."
PROFILE="postgres"

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
