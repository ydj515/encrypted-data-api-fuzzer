#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

ORG="${1:-${ORG:-orgA}}"
SERVICE="${2:-${SERVICE:-reservation}}"
OUTPUT="${3:-path}"
CATALOG_PATH="${GATEWAY_CONTRACT_CATALOG_PATH:-$ROOT_DIR/docs/openapi/gateway/catalog.yaml}"

if [[ "$OUTPUT" != "path" && "$OUTPUT" != "id" ]]; then
  echo "[오류] 출력 형식은 path 또는 id 여야 합니다: $OUTPUT" >&2
  exit 2
fi

if [[ ! -f "$CATALOG_PATH" ]]; then
  echo "[오류] Gateway 계약 카탈로그를 찾을 수 없습니다: $CATALOG_PATH" >&2
  exit 1
fi

CATALOG_DIR="$(cd "$(dirname "$CATALOG_PATH")" && pwd)"

trim_quotes() {
  local value="$1"
  value="${value#\"}"
  value="${value%\"}"
  value="${value#\'}"
  value="${value%\'}"
  printf '%s' "$value"
}

current_id=""
current_org=""
current_service=""
current_path=""
resolved=""

while IFS= read -r line || [[ -n "$line" ]]; do
  case "$line" in
    *"- id:"*)
      current_id="$(trim_quotes "${line#*:}")"
      current_id="${current_id#"${current_id%%[![:space:]]*}"}"
      current_id="${current_id%"${current_id##*[![:space:]]}"}"
      current_org=""
      current_service=""
      current_path=""
      ;;
    *"org:"*)
      if [[ -n "$current_id" ]]; then
        current_org="$(trim_quotes "${line#*:}")"
        current_org="${current_org#"${current_org%%[![:space:]]*}"}"
        current_org="${current_org%"${current_org##*[![:space:]]}"}"
      fi
      ;;
    *"service:"*)
      if [[ -n "$current_id" ]]; then
        current_service="$(trim_quotes "${line#*:}")"
        current_service="${current_service#"${current_service%%[![:space:]]*}"}"
        current_service="${current_service%"${current_service##*[![:space:]]}"}"
      fi
      ;;
    *"openapiPath:"*)
      if [[ -n "$current_id" ]]; then
        current_path="$(trim_quotes "${line#*:}")"
        current_path="${current_path#"${current_path%%[![:space:]]*}"}"
        current_path="${current_path%"${current_path##*[![:space:]]}"}"
        if [[ "$current_org" == "$ORG" && "$current_service" == "$SERVICE" ]]; then
          if [[ "$OUTPUT" == "id" ]]; then
            resolved="$current_id"
          else
            resolved="$current_path"
          fi
          break
        fi
      fi
      ;;
  esac
done < "$CATALOG_PATH"

if [[ -z "$resolved" ]]; then
  echo "[오류] 지원하지 않는 gateway 계약 조합입니다: ORG=$ORG SERVICE=$SERVICE" >&2
  exit 1
fi

if [[ "$OUTPUT" == "path" && "$resolved" != /* ]]; then
  printf '%s\n' "$CATALOG_DIR/$resolved"
else
  printf '%s\n' "$resolved"
fi
