#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

ORG="${1:-${ORG:-orgA}}"
SERVICE="${2:-${SERVICE:-reservation}}"
PROFILE="${3:-${CATS_PROFILE:-full}}"
base_contract="$(bash "$SCRIPT_DIR/resolve-gw-contract.sh" "$ORG" "$SERVICE" path)"

if [[ "$PROFILE" == "smoke" ]]; then
    smoke_contract="${base_contract%.yaml}-smoke.yaml"
    if [[ -f "$smoke_contract" ]]; then
        echo "$smoke_contract"
        exit 0
    fi
fi

echo "$base_contract"
