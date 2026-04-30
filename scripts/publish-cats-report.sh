#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

CATS_REPORTS_BASE="${ROOT_DIR}/cats-report"
REPORT_DATA_DIR="${REPORT_DATA_DIR:-$ROOT_DIR/report-server/data/runs}"
ORG="${ORG:-orgA}"
SERVICE="${SERVICE:-reservation}"
API="${API:-}"
DEFAULT_CONTRACT_PATH="$(bash "$SCRIPT_DIR/resolve-gw-contract.sh" "$ORG" "$SERVICE" path)"
CONTRACT_PATH="${CONTRACT_PATH:-$DEFAULT_CONTRACT_PATH}"
GATEWAY_CONTRACT_CATALOG_PATH="${GATEWAY_CONTRACT_CATALOG_PATH:-$ROOT_DIR/docs/openapi/gateway/catalog.yaml}"

if [ -n "${CATS_REPORT_DIR:-}" ]; then
  REPORT_DIR="$CATS_REPORT_DIR"
else
  REPORT_DIR="$CATS_REPORTS_BASE"
fi

if [ ! -d "$REPORT_DIR" ] || [ ! -f "$REPORT_DIR/cats-summary-report.json" ]; then
  echo "ERROR: 유효한 CATS 리포트 디렉토리가 아닙니다: $REPORT_DIR" >&2
  exit 1
fi

export CATS_REPORT_DIR="$REPORT_DIR"
export REPORT_DATA_DIR
export API
export CONTRACT_PATH
export GATEWAY_CONTRACT_CATALOG_PATH

echo "CATS 리포트: $CATS_REPORT_DIR"
echo "저장소: $REPORT_DATA_DIR"
echo "계약 파일: $CONTRACT_PATH"

if [ -n "${REPORT_PUBLISHER_JAR:-}" ]; then
  java -jar "$REPORT_PUBLISHER_JAR" publish-cats
else
  (cd "$ROOT_DIR/report-server" && ./gradlew -q publishCats)
fi
