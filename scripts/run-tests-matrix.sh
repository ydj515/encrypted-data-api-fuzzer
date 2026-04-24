#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

SOURCE="${SOURCE:-all}"
ORG_FILTER="${ORG:-all}"
SERVICE_FILTER="${SERVICE:-all}"
API_FILTER="${API:-}"
DRY_RUN="${DRY_RUN:-false}"
FAIL_FAST="${FAIL_FAST:-false}"
CATALOG_PATH="${GATEWAY_CONTRACT_CATALOG_PATH:-$ROOT_DIR/docs/openapi/gateway/catalog.yaml}"
SINGLE_SERVICE_RUNNER="${SINGLE_SERVICE_RUNNER:-$SCRIPT_DIR/run-tests.sh}"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'USAGE'
사용법:
  run-tests-matrix.sh [ORG] [SERVICE] [API]

기본 동작:
  - 인자를 주지 않으면 Gateway 계약 카탈로그의 전체 기관/전체 서비스를 순회합니다.
  - SOURCE=all 이 기본값이며 Karate + CATS를 모두 실행합니다.
  - API를 지정하면 해당 API를 제공하는 서비스만 대상으로 실행합니다.

환경변수:
  SOURCE         karate | cats | all (기본: all)
  ORG            기관 필터 (기본: all)
  SERVICE        서비스 필터 (기본: all)
  API            API 필터 (미지정 시 서비스 전체 API)
  DRY_RUN        true면 실제 실행 대신 대상과 명령만 출력
  FAIL_FAST      true면 첫 실패에서 즉시 중단
  GATEWAY_CONTRACT_CATALOG_PATH  Gateway 계약 카탈로그 경로

예시:
  ./scripts/run-tests-matrix.sh
  SOURCE=karate ./scripts/run-tests-matrix.sh
  ./scripts/run-tests-matrix.sh orgB
  ./scripts/run-tests-matrix.sh orgB visit
  SOURCE=cats ./scripts/run-tests-matrix.sh orgB visit listSites
  DRY_RUN=true ./scripts/run-tests-matrix.sh orgA reservation
USAGE
  exit 0
fi

if [[ $# -gt 3 ]]; then
  echo "ERROR: 인자는 최대 3개(ORG SERVICE API)까지만 지원합니다." >&2
  exit 2
fi

if [[ $# -ge 1 ]]; then
  ORG_FILTER="$1"
fi
if [[ $# -ge 2 ]]; then
  SERVICE_FILTER="$2"
fi
if [[ $# -ge 3 ]]; then
  API_FILTER="$3"
fi

normalize_filter() {
  local value="${1:-}"
  local lower=""
  if [[ -z "$value" ]]; then
    printf 'all'
    return
  fi
  lower="$(printf '%s' "$value" | tr '[:upper:]' '[:lower:]')"
  case "$lower" in
    all)
      printf 'all'
      ;;
    *)
      printf '%s' "$value"
      ;;
  esac
}

ORG_FILTER="$(normalize_filter "$ORG_FILTER")"
SERVICE_FILTER="$(normalize_filter "$SERVICE_FILTER")"

case "$SOURCE" in
  karate|cats|all)
    ;;
  *)
    echo "ERROR: SOURCE는 karate, cats, all 중 하나여야 합니다: $SOURCE" >&2
    exit 2
    ;;
esac

if [[ ! -f "$CATALOG_PATH" ]]; then
  echo "ERROR: Gateway 계약 카탈로그를 찾을 수 없습니다: $CATALOG_PATH" >&2
  exit 1
fi

if [[ ! -x "$SINGLE_SERVICE_RUNNER" ]]; then
  echo "ERROR: 단일 서비스 실행 스크립트를 실행할 수 없습니다: $SINGLE_SERVICE_RUNNER" >&2
  exit 1
fi

CATALOG_DIR="$(cd "$(dirname "$CATALOG_PATH")" && pwd)"

trim_value() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  value="${value#\"}"
  value="${value%\"}"
  value="${value#\'}"
  value="${value%\'}"
  printf '%s' "$value"
}

contract_has_api() {
  local contract_path="$1"
  local api="$2"
  if [[ -z "$api" ]]; then
    return 0
  fi
  rg -F -q "/cats/{org}/{service}/$api:" "$contract_path"
}

declare -a entries=()
current_id=""
current_org=""
current_service=""
current_path=""

append_entry() {
  local resolved_path="$current_path"
  if [[ "$resolved_path" != /* ]]; then
    resolved_path="$CATALOG_DIR/$resolved_path"
  fi
  entries+=("$current_id"$'\t'"$current_org"$'\t'"$current_service"$'\t'"$resolved_path")
}

while IFS= read -r line || [[ -n "$line" ]]; do
  case "$line" in
    *"- id:"*)
      current_id="$(trim_value "${line#*:}")"
      current_org=""
      current_service=""
      current_path=""
      ;;
    *"org:"*)
      if [[ -n "$current_id" ]]; then
        current_org="$(trim_value "${line#*:}")"
      fi
      ;;
    *"service:"*)
      if [[ -n "$current_id" ]]; then
        current_service="$(trim_value "${line#*:}")"
      fi
      ;;
    *"openapiPath:"*)
      if [[ -n "$current_id" ]]; then
        current_path="$(trim_value "${line#*:}")"
        if [[ -n "$current_org" && -n "$current_service" && -n "$current_path" ]]; then
          append_entry
        fi
      fi
      ;;
  esac
done < "$CATALOG_PATH"

declare -a selected=()
for entry in "${entries[@]}"; do
  IFS=$'\t' read -r contract_id org service contract_path <<< "$entry"

  if [[ "$ORG_FILTER" != "all" && "$org" != "$ORG_FILTER" ]]; then
    continue
  fi
  if [[ "$SERVICE_FILTER" != "all" && "$service" != "$SERVICE_FILTER" ]]; then
    continue
  fi
  if ! contract_has_api "$contract_path" "$API_FILTER"; then
    continue
  fi

  selected+=("$entry")
done

if [[ "${#selected[@]}" -eq 0 ]]; then
  echo "ERROR: 선택한 조건과 일치하는 Gateway 계약이 없습니다." >&2
  echo "  ORG=${ORG_FILTER}" >&2
  echo "  SERVICE=${SERVICE_FILTER}" >&2
  echo "  API=${API_FILTER:-전체}" >&2
  exit 1
fi

echo "[정보] 서비스 매트릭스 테스트 실행"
echo "  SOURCE=${SOURCE}"
echo "  ORG=${ORG_FILTER}"
echo "  SERVICE=${SERVICE_FILTER}"
echo "  API=${API_FILTER:-전체}"
echo "  DRY_RUN=${DRY_RUN}"
echo "  FAIL_FAST=${FAIL_FAST}"
echo "  MATCHED_SERVICES=${#selected[@]}"

success_count=0
failure_count=0
index=0
declare -a service_results=()
aborted=false

append_service_result() {
  local org="$1"
  local service="$2"
  local result="$3"
  service_results+=("${org}/${service}:${result}")
}

for entry in "${selected[@]}"; do
  IFS=$'\t' read -r contract_id org service contract_path <<< "$entry"
  index=$((index + 1))

  echo
  echo "[정보] [$index/${#selected[@]}] ${org}/${service} (${contract_id})"
  echo "  CONTRACT_PATH=${contract_path}"

  if [[ "$DRY_RUN" == "true" ]]; then
    echo "  COMMAND: SOURCE=$SOURCE ORG=$org SERVICE=$service API=${API_FILTER:-} CONTRACT_PATH=$contract_path $SINGLE_SERVICE_RUNNER"
    success_count=$((success_count + 1))
    append_service_result "$org" "$service" "DRY_RUN"
    continue
  fi

  status=0
  SOURCE="$SOURCE" \
  ORG="$org" \
  SERVICE="$service" \
  API="$API_FILTER" \
  CONTRACT_PATH="$contract_path" \
  GATEWAY_CONTRACT_CATALOG_PATH="$CATALOG_PATH" \
  "$SINGLE_SERVICE_RUNNER" || status=$?

  if [[ "$status" -eq 0 ]]; then
    success_count=$((success_count + 1))
    append_service_result "$org" "$service" "PASS"
    echo "[정보] 완료: ${org}/${service}"
  else
    failure_count=$((failure_count + 1))
    append_service_result "$org" "$service" "FAIL(${status})"
    echo "[오류] 실패: ${org}/${service} (exit=$status)" >&2
    if [[ "$FAIL_FAST" == "true" ]]; then
      aborted=true
      echo "[오류] FAIL_FAST=true 이므로 첫 실패에서 중단합니다." >&2
      break
    fi
  fi
done

summary_status="PASS"
if [[ "$failure_count" -gt 0 ]]; then
  summary_status="FAIL"
fi
if [[ "$aborted" == "true" ]]; then
  summary_status="ABORTED"
fi

summary_services=""
if [[ "${#service_results[@]}" -gt 0 ]]; then
  summary_services="$(printf '%s,' "${service_results[@]}")"
  summary_services="${summary_services%,}"
fi

echo
echo "[정보] 실행 요약"
echo "  TOTAL=${#selected[@]}"
echo "  SUCCESS=${success_count}"
echo "  FAILURE=${failure_count}"
echo "  STATUS=${summary_status}"
echo "  SERVICES=${summary_services:-없음}"
echo "[SUMMARY] source=${SOURCE} org=${ORG_FILTER} service=${SERVICE_FILTER} api=${API_FILTER:-ALL} total=${#selected[@]} success=${success_count} failure=${failure_count} status=${summary_status} fail_fast=${FAIL_FAST} services=${summary_services:-none}"

if [[ "$failure_count" -gt 0 ]]; then
  exit 1
fi
