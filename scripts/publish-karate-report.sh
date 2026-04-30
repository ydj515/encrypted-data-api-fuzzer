#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

KARATE_REPORTS_BASE="$ROOT_DIR/karate-tests/build/karate-reports"
REPORT_DATA_DIR="${REPORT_DATA_DIR:-$ROOT_DIR/report-server/data/runs}"
ORG="${ORG:-orgA}"
SERVICE="${SERVICE:-reservation}"
API="${API:-}"
DEFAULT_CONTRACT_PATH="$(bash "$SCRIPT_DIR/resolve-gw-contract.sh" "$ORG" "$SERVICE" path)"
CONTRACT_PATH="${CONTRACT_PATH:-$DEFAULT_CONTRACT_PATH}"
GATEWAY_CONTRACT_CATALOG_PATH="${GATEWAY_CONTRACT_CATALOG_PATH:-$ROOT_DIR/docs/openapi/gateway/catalog.yaml}"
TEST_CASE_GRANULARITY="${TEST_CASE_GRANULARITY:-${CASE_GRANULARITY:-both}}"

if [ -n "${KARATE_REPORT_DIR:-}" ]; then
  REPORT_DIR="$KARATE_REPORT_DIR"
else
  # 최신 실행 결과 중 실제 케이스 JSON이 존재하는 디렉토리를 우선 사용한다.
  shopt -s nullglob
  CANDIDATES=("$KARATE_REPORTS_BASE"/karate-reports*)
  shopt -u nullglob
  if [ "${#CANDIDATES[@]}" -eq 0 ]; then
    echo "ERROR: Karate 리포트를 찾을 수 없습니다: $KARATE_REPORTS_BASE" >&2
    exit 1
  fi

  REPORT_DIR=""
  while IFS= read -r candidate; do
    if [ -f "$candidate/karate-summary-json.txt" ] && find "$candidate" -maxdepth 1 -name '*.karate-json.txt' -print -quit | grep -q .; then
      REPORT_DIR="$candidate"
      break
    fi
  done < <(ls -td "${CANDIDATES[@]}")

  # 단건/실패 실행 등으로 케이스 JSON이 없을 수도 있으므로 마지막으로 summary만 있는 최신 디렉토리로 fallback 한다.
  if [ -z "$REPORT_DIR" ]; then
    while IFS= read -r candidate; do
      if [ -f "$candidate/karate-summary-json.txt" ]; then
        REPORT_DIR="$candidate"
        break
      fi
    done < <(ls -td "${CANDIDATES[@]}")
  fi
fi

if [ ! -d "$REPORT_DIR" ] || [ ! -f "$REPORT_DIR/karate-summary-json.txt" ]; then
  echo "ERROR: 유효한 Karate 리포트 디렉토리가 아닙니다: $REPORT_DIR" >&2
  exit 1
fi

export KARATE_REPORT_DIR="$REPORT_DIR"
export REPORT_DATA_DIR
export API
export CONTRACT_PATH
export GATEWAY_CONTRACT_CATALOG_PATH
export TEST_CASE_GRANULARITY

echo "Karate 리포트: $KARATE_REPORT_DIR"
echo "저장소: $REPORT_DATA_DIR"
echo "계약 파일: $CONTRACT_PATH"
echo "케이스 단위: $TEST_CASE_GRANULARITY"

if [ -n "${REPORT_PUBLISHER_JAR:-}" ]; then
  java -jar "$REPORT_PUBLISHER_JAR" publish-karate
else
  (cd "$ROOT_DIR/report-server" && ./gradlew -q publishKarate)
fi
