#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ORG="${ORG:-orgA}"
SERVICE="${SERVICE:-reservation}"
API="${API:-}"
DEFAULT_CONTRACT_PATH="$(bash "$SCRIPT_DIR/resolve-cats-contract.sh" "$ORG" "$SERVICE" full)"
CONTRACT_PATH="${CONTRACT_PATH:-$DEFAULT_CONTRACT_PATH}"
SERVER_URL="${SERVER_URL:-http://localhost:28080}"
CATS_BIN="${CATS_BIN:-cats}"
DRY_RUN="${DRY_RUN:-false}"
BLACKBOX="${BLACKBOX:-false}"
SKIP_IGNORED_REPORTING="${SKIP_IGNORED_REPORTING:-false}"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<USAGE
사용법:
  $(basename "$0") [추가 cats 인자...]

환경변수(선택):
  CONTRACT_PATH  OpenAPI 파일 경로 (기본: ORG/SERVICE에 맞는 gateway 계약 자동 선택)
  SERVER_URL     Gateway 서버 URL (기본: http://localhost:28080)
  ORG            path 변수 org 값 (기본: orgA)
  SERVICE        path 변수 service 값 (기본: reservation)
  API            실행 대상 API 이름 (미지정 시 전체, 예: createReservation)
  CATS_BIN       cats 실행 파일 경로/이름 (기본: cats)
  DRY_RUN        true면 실행하지 않고 명령만 출력
  BLACKBOX       true면 5xx만 에러로 보는 blackbox 모드 사용 (기본: false)
  SKIP_IGNORED_REPORTING true면 ignore된 응답을 리포트에서 생략 (기본: false)
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
  -P "org=$ORG"
  -P "service=$SERVICE"
)

if [[ -n "$API" ]]; then
  cmd+=(--path "/cats/{org}/{service}/$API")
fi

if [[ "$BLACKBOX" == "true" ]]; then
  cmd+=(-b)
fi

if [[ "$SKIP_IGNORED_REPORTING" == "true" ]]; then
  cmd+=(-k)
fi

if [[ $# -gt 0 ]]; then
  cmd+=("$@")
fi

echo "[정보] FULL 모드 실행 명령:"
echo "[정보] 계약 파일: $CONTRACT_PATH"
printf ' %q' "${cmd[@]}"
echo

if [[ "$DRY_RUN" == "true" ]]; then
  echo "[정보] DRY_RUN=true 이므로 실행하지 않습니다."
  exit 0
fi

"${cmd[@]}"
