#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "[정보] run-cats-gw.sh는 full 모드 래퍼입니다."
exec "$SCRIPT_DIR/run-cats-gw-full.sh" "$@"
