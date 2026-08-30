#!/usr/bin/env bash
set -e

echo "==================================================================="
echo " Transport & Logistics Modular Monolith - Application Runner"
echo "==================================================================="
echo ""

# Detect Java 21 across macOS (Apple Silicon / Intel) and Linux / Ubuntu
detect_java() {
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        if "$JAVA_HOME/bin/java" -version 2>&1 | grep -q '"21'; then
            return 0
        fi
    fi

    # macOS Apple Silicon / Intel
    if command -v /usr/libexec/java_home >/dev/null 2>&1; then
        MACOS_JAVA=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
        if [ -n "$MACOS_JAVA" ] && [ -d "$MACOS_JAVA" ]; then
            export JAVA_HOME="$MACOS_JAVA"
            export PATH="$JAVA_HOME/bin:$PATH"
            return 0
        fi
    fi

    # Homebrew on macOS (Apple Silicon / Intel)
    for brew_path in "/opt/homebrew/opt/openjdk@21" "/usr/local/opt/openjdk@21"; do
        if [ -d "$brew_path" ]; then
            export JAVA_HOME="$brew_path"
            export PATH="$JAVA_HOME/bin:$PATH"
            return 0
        fi
    done

    # Linux / Ubuntu standard locations
    for jvm_path in \
        "/usr/lib/jvm/java-21-openjdk-amd64" \
        "/usr/lib/jvm/java-21-openjdk-arm64" \
        "/usr/lib/jvm/java-1.21.0-openjdk-amd64" \
        "/usr/lib/jvm/jdk-21.0.12.1-oracle-x64" \
        "/usr/lib/jvm/temurin-21-jdk-amd64" \
        "/usr/lib/jvm/temurin-21-jdk-arm64" \
        "/usr/lib/jvm/java-21-amazon-corretto" \
        "/usr/lib/jvm/zulu-21-amd64" \
        "/usr/lib/jvm/zulu-21-arm64"; do
        if [ -d "$jvm_path" ]; then
            export JAVA_HOME="$jvm_path"
            export PATH="$JAVA_HOME/bin:$PATH"
            return 0
        fi
    done

    if command -v java >/dev/null 2>&1; then
        return 0
    fi

    echo "[WARNING] JDK 21 not automatically detected. Relying on default system Java."
}

detect_java

# Resolve Maven executable (system mvn or project wrapper)
if [ -x "./mvnw" ]; then
    MVN_CMD="./mvnw"
elif command -v mvn >/dev/null 2>&1; then
    MVN_CMD="mvn"
else
    echo "[!] Maven or ./mvnw not found. Please install Maven or make ./mvnw executable."
    exit 1
fi

check_port() {
    local port=$1
    if command -v nc >/dev/null 2>&1; then
        nc -z 127.0.0.1 "$port" 2>/dev/null || nc -z localhost "$port" 2>/dev/null
    elif command -v lsof >/dev/null 2>&1; then
        lsof -i :"$port" >/dev/null 2>&1
    else
        (echo > /dev/tcp/127.0.0.1/"$port") 2>/dev/null
    fi
}

for port in 8080 5173; do
    if check_port "$port"; then
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
if ! check_port "${POSTGRES_PORT:-5432}"; then
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
"$MVN_CMD" spring-boot:run -Dspring-boot.run.profiles=${PROFILE} -Dspring-boot.run.jvmArguments="-Dapp.dev.identity-bootstrap.enabled=true -Dapp.dev.identity-bootstrap.username=admin -Dapp.dev.identity-bootstrap.password=AdminPassword123! -Dapp.dev.sample-data.enabled=true" &
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
