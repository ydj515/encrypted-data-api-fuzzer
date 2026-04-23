#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

ORG="${1:-${ORG:-catsOrg}}"
SERVICE="${2:-${SERVICE:-booking}}"
PROFILE="${3:-${CATS_PROFILE:-full}}"

case "$ORG/$SERVICE" in
  catsOrg/booking)
    base_contract="$ROOT_DIR/docs/openapi/gateway/cats-gw-openapi.yaml"
    ;;
  orgA/A)
    base_contract="$ROOT_DIR/docs/openapi/gateway/orgA-A-gw-openapi.yaml"
    ;;
  orgB/visit)
    base_contract="$ROOT_DIR/docs/openapi/gateway/orgB-visit-gw-openapi.yaml"
    ;;
  orgB/support)
    base_contract="$ROOT_DIR/docs/openapi/gateway/orgB-support-gw-openapi.yaml"
    ;;
  *)
    echo "[오류] 지원하지 않는 CATS gateway 계약 조합입니다: ORG=$ORG SERVICE=$SERVICE" >&2
    exit 1
    ;;
esac

if [[ "$PROFILE" == "smoke" ]]; then
    smoke_contract="${base_contract%.yaml}-smoke.yaml"
    if [[ -f "$smoke_contract" ]]; then
        echo "$smoke_contract"
        exit 0
    fi
fi

echo "$base_contract"
