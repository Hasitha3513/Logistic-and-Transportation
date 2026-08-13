#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
./mvnw clean test
cd ..
zip -qr transport-logistics-modulith.zip transport-logistics-modulith -x '*/target/*'
echo "Created transport-logistics-modulith.zip"
