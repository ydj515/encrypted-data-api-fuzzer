---
name: "generate-karate-from-openapi"
description: "OpenAPI/Swagger YAML 문서만을 기준으로 이 저장소의 Karate feature 테스트를 생성하거나 업데이트하는 스킬이다. 사용자가 `docs/openapi/mock-rest-api-server/*.yaml`, Swagger 기반 Karate 생성, gateway action path용 feature, 단일 API 테스트, 시나리오 기반 Karate 흐름, negative case 보강, org/service/api 추론, response-to-request 체인 탐색 중 하나라도 언급하면 이 스킬을 우선 사용한다. mock 구현 코드나 seed 데이터에서 검증 규칙을 추론하지 않고 Swagger와 현재 REST API 스펙만 source of truth로 유지한다."
compatibility: "Python 3, PyYAML, and access to this repository's docs/openapi and karate-tests directories."
---

# Generate Karate From OpenAPI

> **싱크 안내:** 이 파일을 수정한 후에는 `docs/skills/generate-karate-from-openapi/SKILL-SYNC.md`를 참조하여 `.claude/skills/generate-karate-from-openapi/`의 대응 항목도 함께 업데이트하세요.

## Overview

mock REST OpenAPI 스펙에서 이 저장소의 컨벤션을 지키며 Karate 테스트를 생성합니다: gateway 방식의 POST 호출, `@service`·`@api` 태그, `karate-tests/src/test/resources/scenarios` 하위 파일 구조.

시나리오 기반 테스트를 생성할 때는 고정된 lifecycle 템플릿을 그대로 출력하지 않습니다. 스코프를 하나의 Swagger/OpenAPI 문서(= 하나의 org/service)로 제한합니다. OpenAPI 경로, 요청/응답 필드, operation 요약/설명, examples, defaults, 기존 Karate 스타일만 검사하여 이전 응답 값이 다음 요청의 의미적으로 유효한 입력인 API 체인을 선택합니다.

단일 API 테스트와 시나리오 테스트는 리포트와 필터링에서 구분되도록 별도 `@api` 식별자를 사용합니다. 단일 API 테스트는 실제 gateway action 이름(`createReservation`)을 쓰고, 시나리오 테스트는 흐름 전용 식별자(`reservationLifecycle`, `resourceScheduleReservationFlow`)를 사용합니다.

이 저장소의 Karate 기준 버전은 `1.5.2`입니다. Gradle/Maven 좌표는 `io.karatelabs:karate-junit5:1.5.2`를 기준으로 보고, Java import는 당분간 기존 `com.intuit.karate.*` 패키지명을 유지합니다. `1.5.x`에서는 `@setup` lifecycle을 사용할 수 있으므로, 순수한 business flow가 아닌 준비용 데이터 생성은 가능한 한 `@setup`이나 보조 helper 호출로 분리할 수 있는지 먼저 검토합니다.

## Quick Start

저장소 루트에서:

```bash
python3 .agents/skills/generate-karate-from-openapi/scripts/generate_karate_features.py \
  --openapi docs/openapi/mock-rest-api-server/orgB-support-plain-api.yaml \
  --mode summary
```

단일 API 테스트 생성:

```bash
python3 .agents/skills/generate-karate-from-openapi/scripts/generate_karate_features.py \
  --openapi docs/openapi/mock-rest-api-server/orgB-support-plain-api.yaml \
  --mode api \
  --api listDevices
```

데이터 흐름 후보 확인 (시나리오 설계 전):

```bash
python3 .agents/skills/generate-karate-from-openapi/scripts/generate_karate_features.py \
  --openapi docs/openapi/mock-rest-api-server/orgB-visit-plain-api.yaml \
  --mode graph
```

`--dry-run`: 파일 작성 전 생성 텍스트 확인. `--skip-existing`: 누락된 단일 API 파일만 일괄 생성. `--overwrite`: 기존 테스트를 읽고 교체 의도를 확인한 후에만 사용.

## Workflow

1. `karate-tests/AGENTS.md`, `karate-tests/src/test/resources/karate-config.js`, 인근 `.feature` 파일을 읽어 스타일을 파악한다.
2. `docs/openapi/mock-rest-api-server/`에서 대상 OpenAPI YAML을 검사한다.
3. `--mode summary`로 생성기를 실행하여 추론된 `org`, `service`, gateway API 이름, 요청/응답 필드 목록을 확인한다.
4. 생성 모드를 선택한다:
   - 단일 API: `--mode api --api <apiName>` 또는 `--api` 생략 시 추론된 모든 API를 스캐폴딩. 누락 파일만 생성 시 `--skip-existing` 추가.
   - 시나리오: 선택한 하나의 OpenAPI 문서에 `--mode graph`를 실행하여 response-to-request 후보를 찾은 후, `references/scenario-planning.md`를 읽고 LLM 판단으로 시나리오를 구성. 이미 인식된 create-detail-terminal 흐름에 대해서는 `--mode scenario`를 보수적 부트스트랩으로만 사용.
5. 생성된 파일을 동일한 OpenAPI 문서 및 인근 `.feature` 스타일과 대조 검토한다. 시나리오 테스트에서는 `* def`로 응답 값을 명시적으로 캡처하고 `#(...)`으로 이후 요청 바디에 공급한다.
6. 서비스가 이용 가능한 경우 집중 Karate 테스트를 실행한다:

```bash
cd karate-tests
ORG=<org> SERVICE=<service> API=<apiName> ./gradlew test
```

upstream 서비스를 사용할 수 없는 경우에도 기존 Karate 패턴과 대조하여 생성된 feature 문법을 검증한다.

## Repository Conventions

경로, 태그, 명명, API 매핑, 검증 세부 사항이 필요하면 `references/project-karate-patterns.md`를 로드한다.

API 체인이나 응답 값 재사용을 고려한 시나리오 기반 테스트를 만들기 전에 `references/scenario-planning.md`를 로드한다.

필수 필드, enum, format, 범위 검증 케이스로 단일 API 테스트를 확장하기 전에 `references/negative-case-generation.md`를 로드한다.

핵심 규칙:

- `karate-tests/src/test/resources/scenarios/<service>/<case-name>/<case-name>.feature` 하위에 feature를 작성한다.
- `KarateRunner`가 `SERVICE`, `API`로 필터할 수 있도록 태그를 사용한다. 단일 API 테스트는 `@service=support @api=createTicket`, 시나리오 테스트는 `@service=support @api=ticketLifecycle`처럼 흐름 전용 `@api`를 사용한다.
- `KarateRunner` tag 필터는 Karate `1.5.x` 규칙에 맞춰 태그별 조건으로 조합된다고 가정한다. 하나의 문자열에 `and`를 직접 이어 붙이는 방식보다 태그를 개별 조건으로 유지하는 편을 우선한다.
- `Background`에 `url gatewayUrl`, 고정 `org`, 고정 `service`, `basePath = '/cats/' + org + '/' + service`를 설정한다.
- plain mock-rest-api 경로가 아닌 gateway action 경로를 호출한다: `Given path basePath + '/createTicket'`.
- gateway 테스트에는 plain API의 path variable을 요청 필드로 포함하는 JSON 요청 바디와 함께 POST를 사용한다.
- 요청 샘플에는 OpenAPI `example`, `examples`, `default`, enum 값을 우선 사용한다. Swagger에 예시가 없는 경우에만 결정론적 스키마 유효 폴백 값을 사용한다.
- 시나리오 테스트에서는 의미 있는 응답 값을 캡처하고, 다음 API의 요청 필드가 같은 도메인 의미를 가질 때만 `#(...)`으로 재사용한다.
- 단일 API 테스트에는 `@kind=single-api`, 시나리오 테스트에는 `@kind=scenario` 태그를 함께 붙여 의도를 드러낸다.
- 준비용 setup 호출이 필요한 경우 Karate `1.5.x`의 `@setup` lifecycle이나 보조 helper를 먼저 고려하고, business flow 자체를 검증하는 경우에만 하나의 시나리오 안에 여러 API를 연쇄 호출한다.
- 다른 Swagger/OpenAPI 문서나 다른 서비스에 걸친 API 체인으로 시나리오 테스트를 구성하지 않는다.

## Script Notes

`scripts/generate_karate_features.py`는 Python 3와 PyYAML이 필요합니다. PyYAML이 없으면 `python3 -m pip install pyyaml` 후 재실행하거나, 스크립트를 가독 템플릿으로 활용하여 feature를 수동 작성합니다.

스크립트는 의도적으로 보수적인 스캐폴드를 생성합니다. 생성 후 다음을 보강합니다:

- OpenAPI 필수 요청 필드별 누락 필드 부정 시나리오 1개
- OpenAPI 요청 스키마의 enum, format, 범위, 문자열 길이, 패턴 부정 시나리오
- `OPEN`, `RESOLVED`, `REQUESTED`, `CANCELED`, `CREATED` 같은 정확한 상태 전환
- `totalQuantity == availableQuantity + reservedQuantity` 같은 집계 어설션
- 생성된 ID가 필요한 단일 API에 대한 setup 호출

단일 API 테스트에서 스크립트는 필수 요청 ID가 다른 API에서 생성될 때 setup/cleanup 호출을 추가할 수 있습니다. 대상 API가 유일한 테스트 대상이고 나머지 호출은 setup/cleanup 전용인 경우 해당 파일을 단일 API 테스트로 취급합니다.

## Validation

스킬 구조를 빠르게 검증합니다:

```bash
python3 "$HOME/.agents/skills/skill-creator/scripts/quick_validate.py" \
  .agents/skills/generate-karate-from-openapi
```

생성된 테스트를 가능한 좁은 스코프로 검증합니다:

```bash
cd karate-tests
ORG=orgB SERVICE=support API=listDevices ./gradlew test
ORG=orgA SERVICE=reservation API=reservationLifecycle ./gradlew test
```
