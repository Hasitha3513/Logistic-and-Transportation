#!/usr/bin/env bash
set -e

# Detect Java 21 across macOS (Apple Silicon / Intel) and Linux / Ubuntu
if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
    if command -v /usr/libexec/java_home >/dev/null 2>&1; then
        MACOS_JAVA=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
        if [ -n "$MACOS_JAVA" ]; then
            export JAVA_HOME="$MACOS_JAVA"
            export PATH="$JAVA_HOME/bin:$PATH"
        fi
    else
        for jvm in \
            "/usr/lib/jvm/java-21-openjdk-amd64" \
            "/usr/lib/jvm/java-21-openjdk-arm64" \
            "/usr/lib/jvm/java-1.21.0-openjdk-amd64" \
            "/usr/lib/jvm/jdk-21.0.12.1-oracle-x64" \
            "/opt/homebrew/opt/openjdk@21" \
            "/usr/local/opt/openjdk@21"; do
            if [ -d "$jvm" ]; then
                export JAVA_HOME="$jvm"
                export PATH="$JAVA_HOME/bin:$PATH"
                break
            fi
        done
    fi
fi

# Resolve Maven executable
if [ -x "./mvnw" ]; then
    MVN_CMD="./mvnw"
elif command -v mvn >/dev/null 2>&1; then
    MVN_CMD="mvn"
else
    echo "[!] Maven or ./mvnw not found."
    exit 1
fi

echo "=== Transport & Logistics Static Analysis ==="

echo "1. Compile + Tests"
"$MVN_CMD" clean verify

echo "2. SpotBugs"
"$MVN_CMD" spotbugs:check

echo "3. PMD"
"$MVN_CMD" pmd:check

echo "4. Checkstyle"
"$MVN_CMD" checkstyle:check

echo "5. OWASP Dependency Check"
"$MVN_CMD" org.owasp:dependency-check-maven:check

echo "=== STATIC ANALYSIS PASSED ==="