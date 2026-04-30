#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
KARATE_BUILD_DIR="$ROOT_DIR/karate-tests/build/karate-reports"

ORG="${ORG:-orgA}"
SERVICE="${SERVICE:-reservation}"
API="${API:-}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:28080}"

export ORG
export SERVICE
export API
export GATEWAY_URL

echo "[정보] Karate 시나리오 실행"
echo "  ORG=${ORG}"
echo "  SERVICE=${SERVICE}"
echo "  API=${API:-전체}"
echo "  GATEWAY_URL=${GATEWAY_URL}"

rm -rf "$KARATE_BUILD_DIR"

(cd "$ROOT_DIR/karate-tests" && ./gradlew test --rerun-tasks)
