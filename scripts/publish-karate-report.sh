#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

KARATE_REPORTS_BASE="$ROOT_DIR/karate-tests/build/karate-reports"
REPORT_DATA_DIR="${REPORT_DATA_DIR:-$ROOT_DIR/report-server/data/runs}"
ORG="${ORG:-catsOrg}"
SERVICE="${SERVICE:-booking}"
API="${API:-}"
TEST_CASE_GRANULARITY="${TEST_CASE_GRANULARITY:-${CASE_GRANULARITY:-both}}"

if [ -n "${KARATE_REPORT_DIR:-}" ]; then
  REPORT_DIR="$KARATE_REPORT_DIR"
else
  # Karate의 현재 실행 결과 디렉토리를 우선 사용하고, 없으면 수정 시각이 가장 최신인 백업 디렉토리를 사용한다.
  FIXED_REPORT_DIR="$KARATE_REPORTS_BASE/karate-reports"
  if [ -d "$FIXED_REPORT_DIR" ]; then
    REPORT_DIR="$FIXED_REPORT_DIR"
  else
    shopt -s nullglob
    CANDIDATES=("$KARATE_REPORTS_BASE"/karate-reports*)
    shopt -u nullglob
    if [ "${#CANDIDATES[@]}" -eq 0 ]; then
      echo "ERROR: Karate 리포트를 찾을 수 없습니다: $KARATE_REPORTS_BASE" >&2
      exit 1
    fi
    REPORT_DIR="$(ls -td "${CANDIDATES[@]}" | head -n 1)"
  fi
fi

if [ ! -d "$REPORT_DIR" ] || [ ! -f "$REPORT_DIR/karate-summary-json.txt" ]; then
  echo "ERROR: 유효한 Karate 리포트 디렉토리가 아닙니다: $REPORT_DIR" >&2
  exit 1
fi

export KARATE_REPORT_DIR="$REPORT_DIR"
export REPORT_DATA_DIR
export ORG
export SERVICE
export API
export TEST_CASE_GRANULARITY

echo "Karate 리포트: $KARATE_REPORT_DIR"
echo "저장소: $REPORT_DATA_DIR"
echo "케이스 단위: $TEST_CASE_GRANULARITY"

if [ -n "${REPORT_PUBLISHER_JAR:-}" ]; then
  java -jar "$REPORT_PUBLISHER_JAR" publish-karate
else
  (cd "$ROOT_DIR/report-server" && ./gradlew -q publishKarate)
fi
