#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SOURCE="${SOURCE:-all}"

run_karate() {
  local status=0
  "$SCRIPT_DIR/run-karate.sh" || status=$?
  "$SCRIPT_DIR/publish-karate-report.sh" || {
    local publish_status=$?
    if [ "$status" -eq 0 ]; then
      return "$publish_status"
    fi
  }
  return "$status"
}

run_cats() {
  local status=0
  "$SCRIPT_DIR/run-cats.sh" || status=$?
  "$SCRIPT_DIR/publish-cats-report.sh" || {
    local publish_status=$?
    if [ "$status" -eq 0 ]; then
      return "$publish_status"
    fi
  }
  return "$status"
}

echo "[정보] 통합 테스트 실행"
echo "  SOURCE=${SOURCE}"
echo "  ORG=${ORG:-catsOrg}"
echo "  SERVICE=${SERVICE:-전체}"
echo "  API=${API:-전체}"

case "$SOURCE" in
  karate)
    run_karate
    ;;
  cats)
    run_cats
    ;;
  all)
    run_karate
    run_cats
    ;;
  *)
    echo "ERROR: SOURCE는 karate, cats, all 중 하나여야 합니다: $SOURCE" >&2
    exit 2
    ;;
esac
