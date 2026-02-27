#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONTRACT_PATH="${CONTRACT_PATH:-$ROOT_DIR/docs/openapi/cats-gw-smoke-openapi.yaml}"
SERVER_URL="${SERVER_URL:-http://localhost:28080}"
ORG="${ORG:-catsOrg}"
SERVICE="${SERVICE:-booking}"
CATS_BIN="${CATS_BIN:-cats}"
DRY_RUN="${DRY_RUN:-false}"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<USAGE
사용법:
  $(basename "$0") [추가 cats 인자...]

환경변수(선택):
  CONTRACT_PATH  OpenAPI 파일 경로 (기본: docs/openapi/cats-gw-smoke-openapi.yaml)
  SERVER_URL     Gateway 서버 URL (기본: http://localhost:28080)
  ORG            path 변수 org 값 (기본: catsOrg)
  SERVICE        path 변수 service 값 (기본: booking)
  CATS_BIN       cats 실행 파일 경로/이름 (기본: cats)
  DRY_RUN        true면 실행하지 않고 명령만 출력
USAGE
  exit 0
fi

if [[ ! -f "$CONTRACT_PATH" ]]; then
  echo "[오류] OpenAPI 파일을 찾을 수 없습니다: $CONTRACT_PATH" >&2
  exit 1
fi

if ! command -v "$CATS_BIN" >/dev/null 2>&1; then
  echo "[오류] cats 실행 파일을 찾을 수 없습니다: $CATS_BIN" >&2
  echo "설치 예시: brew tap endava/tap && brew install cats" >&2
  exit 1
fi

cmd=(
  "$CATS_BIN"
  -c "$CONTRACT_PATH"
  -s "$SERVER_URL"
  -b
  -k
  -P "org=$ORG"
  -P "service=$SERVICE"
)

if [[ $# -gt 0 ]]; then
  cmd+=("$@")
fi

echo "[정보] SMOKE 모드 실행 명령:"
printf ' %q' "${cmd[@]}"
echo

if [[ "$DRY_RUN" == "true" ]]; then
  echo "[정보] DRY_RUN=true 이므로 실행하지 않습니다."
  exit 0
fi

"${cmd[@]}"
