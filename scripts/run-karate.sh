#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

ORG="${ORG:-catsOrg}"
SERVICE="${SERVICE:-booking}"
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

(cd "$ROOT_DIR/karate-tests" && ./gradlew test)
