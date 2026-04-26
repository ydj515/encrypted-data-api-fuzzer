---
name: generate-karate-from-openapi
description: OpenAPI/Swagger YAML 문서만을 기준으로 이 저장소의 Karate feature 테스트를 생성하거나 업데이트한다. karate-tests/src/test/resources/scenarios 하위에 단일 API 모드 또는 LLM 계획 시나리오 모드로 테스트를 작성할 때 사용한다. org/service/api 이름은 OpenAPI 경로와 operation 메타데이터에서 추론하고, 요청 바디와 응답 어설션을 스캐폴딩하며, 동일 문서 내 response-to-request 데이터 흐름 후보를 탐색한다. mock 구현 코드에서 테스트 입력이나 검증 규칙을 추론하지 않는다. Swagger와 현재 REST API 스펙을 유일한 진실 소스로 유지한다.
---

# Generate Karate From OpenAPI

> **싱크 안내:** 이 파일을 수정한 후에는 `docs/skills/generate-karate-from-openapi/SKILL-SYNC.md`를 참조하여 `.codex/skills/generate-karate-from-openapi/`의 대응 항목도 함께 업데이트하세요.

## Overview

mock REST OpenAPI 스펙에서 이 저장소의 컨벤션을 지키며 Karate 테스트를 생성합니다: gateway 방식의 POST 호출, `@service`·`@api` 태그, `karate-tests/src/test/resources/scenarios` 하위 파일 구조.

시나리오 기반 테스트를 생성할 때는 고정된 lifecycle 템플릿을 그대로 출력하지 않습니다. 스코프를 하나의 Swagger/OpenAPI 문서(= 하나의 org/service)로 제한합니다. OpenAPI 경로, 요청/응답 필드, operation 요약/설명, examples, defaults, 기존 Karate 스타일만 검사하여 이전 응답 값이 다음 요청의 의미적으로 유효한 입력인 API 체인을 선택합니다.

## Quick Start

저장소 루트에서:

```bash
python3 .claude/skills/generate-karate-from-openapi/scripts/generate_karate_features.py \
  --openapi docs/openapi/mock-rest-api-server/orgB-support-plain-api.yaml \
  --mode summary
```

단일 API 테스트 생성:

```bash
python3 .claude/skills/generate-karate-from-openapi/scripts/generate_karate_features.py \
  --openapi docs/openapi/mock-rest-api-server/orgB-support-plain-api.yaml \
  --mode api \
  --api listDevices
```

데이터 흐름 후보 확인 (시나리오 설계 전):

```bash
python3 .claude/skills/generate-karate-from-openapi/scripts/generate_karate_features.py \
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
- `KarateRunner`가 `SERVICE`, `API`로 필터할 수 있도록 `@service=support @api=createTicket` 같은 태그를 사용한다.
- `Background`에 `url gatewayUrl`, 고정 `org`, 고정 `service`, `basePath = '/cats/' + org + '/' + service`를 설정한다.
- plain mock-rest-api 경로가 아닌 gateway action 경로를 호출한다: `Given path basePath + '/createTicket'`.
- gateway 테스트에는 plain API의 path variable을 요청 필드로 포함하는 JSON 요청 바디와 함께 POST를 사용한다.
- 요청 샘플에는 OpenAPI `example`, `examples`, `default`, enum 값을 우선 사용한다. Swagger에 예시가 없는 경우에만 결정론적 스키마 유효 폴백 값을 사용한다.
- 시나리오 테스트에서는 의미 있는 응답 값을 캡처하고, 다음 API의 요청 필드가 같은 도메인 의미를 가질 때만 `#(...)`으로 재사용한다.
- 다른 Swagger/OpenAPI 문서나 다른 서비스에 걸친 API 체인으로 시나리오 테스트를 구성하지 않는다.

## Script Notes

`scripts/generate_karate_features.py`는 Python 3와 PyYAML이 필요합니다. PyYAML이 없으면 `python3 -m pip install pyyaml` 후 재실행하거나, 스크립트를 가독 템플릿으로 활용하여 feature를 수동 작성합니다.

스크립트는 의도적으로 보수적인 스캐폴드를 생성합니다. 생성 후 다음을 보강합니다:

- OpenAPI 필수 요청 필드별 누락 필드 부정 시나리오 1개
- OpenAPI 요청 스키마의 enum, format, 범위 부정 시나리오
- `OPEN`, `RESOLVED`, `REQUESTED`, `CANCELED`, `CREATED` 같은 정확한 상태 전환
- `totalQuantity == availableQuantity + reservedQuantity` 같은 집계 어설션
- 생성된 ID가 필요한 단일 API에 대한 setup 호출

단일 API 테스트에서 스크립트는 필수 요청 ID가 다른 API에서 생성될 때 setup/cleanup 호출을 추가할 수 있습니다. 대상 API가 유일한 테스트 대상이고 나머지 호출은 setup/cleanup 전용인 경우 해당 파일을 단일 API 테스트로 취급합니다.

## Validation

생성된 테스트를 가능한 좁은 스코프로 검증합니다:

```bash
cd karate-tests
ORG=orgB SERVICE=support API=listDevices ./gradlew test
```
