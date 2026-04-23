#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

CATS_PROFILE="${CATS_PROFILE:-full}"

case "$CATS_PROFILE" in
  full)
    exec "$SCRIPT_DIR/run-cats-gw-full.sh" "$@"
    ;;
  smoke)
    exec "$SCRIPT_DIR/run-cats-gw-smoke.sh" "$@"
    ;;
  *)
    echo "ERROR: CATS_PROFILE은 full 또는 smoke만 지원합니다: $CATS_PROFILE" >&2
    exit 2
    ;;
esac
