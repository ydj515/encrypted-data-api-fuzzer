# OpenAPI 기반 Karate 생성 규칙

이 문서는 `docs/openapi/mock-rest-api-server/*.yaml` Swagger/OpenAPI 문서를 기준으로 Karate feature를 생성하는 규칙을 정리합니다. 테스트 입력값, 시나리오 흐름, negative case는 Swagger를 source of truth로 삼습니다.

## 기준 문서와 원칙

Karate feature는 mock REST API 서버의 Swagger 문서를 기준으로 생성합니다.

```text
docs/openapi/mock-rest-api-server/*.yaml
```

mock 구현 코드, 임시 seed 데이터, controller/service 내부 검증 로직은 테스트 입력값이나 negative rule의 근거로 사용하지 않습니다. API 동작과 Swagger가 다르면 Swagger를 먼저 수정해 현재 REST API spec과 싱크를 맞춘 뒤 테스트를 생성합니다.

생성 스킬은 이 워크스페이스의 `.codex/skills/generate-karate-from-openapi`에 있습니다.

```bash
python3 .codex/skills/generate-karate-from-openapi/scripts/generate_karate_features.py \
  --openapi docs/openapi/mock-rest-api-server/orgB-visit-plain-api.yaml \
  --mode api \
  --overwrite
```

생성된 feature는 아래 경로에 서비스별로 저장합니다.

```text
karate-tests/src/test/resources/scenarios/
```

## 생성 모드

생성 방식은 크게 단건 API와 시나리오 기반으로 나뉩니다.

| 구분 | 생성 기준 | 예시 |
|---|---|---|
| 단건 API feature | Swagger operation별 request/response schema | `listResources`, `createVisit` |
| 시나리오 feature | 하나의 Swagger 문서 안에서 응답값을 다음 요청에 재사용할 수 있는 흐름 | site 목록 응답의 `siteId`로 slot 확인 후 visit 생성 |

### 단건 API feature

단건 API feature는 하나의 operation을 검증 대상으로 둡니다. 필요한 setup/cleanup 호출이 있을 수 있지만, 검증의 중심은 `@api=<apiName>` 태그로 지정된 하나의 API여야 합니다.

대표 실행:

```bash
python3 .codex/skills/generate-karate-from-openapi/scripts/generate_karate_features.py \
  --openapi docs/openapi/mock-rest-api-server/orgB-support-plain-api.yaml \
  --mode api \
  --api listDevices \
  --overwrite
```

기존에 없는 단건 API feature만 보강할 때는 `--skip-existing`을 사용합니다.

```bash
python3 .codex/skills/generate-karate-from-openapi/scripts/generate_karate_features.py \
  --openapi docs/openapi/mock-rest-api-server/orgB-support-plain-api.yaml \
  --mode api \
  --skip-existing
```

### 시나리오 기반 feature

시나리오 기반 feature는 LLM 판단으로 API 간 데이터 흐름을 구성합니다. 단, 범위는 하나의 Swagger 문서, 즉 하나의 서비스 안으로 제한합니다.

먼저 `--mode graph`로 응답 필드와 요청 필드의 연결 후보를 확인합니다.

```bash
python3 .codex/skills/generate-karate-from-openapi/scripts/generate_karate_features.py \
  --openapi docs/openapi/mock-rest-api-server/orgB-visit-plain-api.yaml \
  --mode graph
```

이후 같은 Swagger 문서 안에서만 아래 조건을 만족하는 흐름을 선택합니다.

- 이전 API 응답 필드와 다음 API 요청 필드의 도메인 의미가 같거나 직접 호환됩니다.
- operation summary, description, schema name, field name, example 값이 같은 업무 흐름을 가리킵니다.
- 다른 org/service 또는 다른 Swagger 문서의 API를 섞지 않습니다.
- response-only 필드를 억지로 request 입력값으로 쓰지 않습니다.

예를 들어 site 목록 응답의 `siteId`가 slot 조회나 visit 생성 요청의 `siteId`와 같은 의미라면, 첫 응답에서 값을 캡처해 다음 요청에 `#(...)`로 주입할 수 있습니다.

## Gateway 호출 규칙

Gateway 테스트는 plain REST path를 직접 호출하지 않고 `/cats/{org}/{service}/{apiName}` 형태의 action path를 POST로 호출합니다.

```gherkin
Given path basePath + '/createVisit'
And request requestBody
When method post
Then status 200
```

plain path parameter는 Gateway 요청 JSON body field로 넣습니다. 예를 들어 plain API가 `GET /sites/{siteId}/slots`라면 Karate Gateway 요청은 action path를 POST로 호출하고 `siteId`를 request body에 포함합니다.

## 입력값 생성 규칙

요청 샘플은 Swagger request schema에서만 추론합니다.

| 기준 | 생성 규칙 |
|---|---|
| `example` / `examples` | 가장 우선 사용합니다. |
| `default` | example이 없을 때 정상값 후보로 사용합니다. |
| `enum` | 허용값 중 첫 번째 값을 정상 요청에 사용합니다. |
| `format` | `date`, `date-time`, `uuid`, `email` 등 format에 맞는 deterministic 값을 사용합니다. |
| `minimum` / `maximum` | 범위 안의 값을 정상 요청에 사용합니다. |
| schema type | 예시가 없으면 type에 맞는 보수적인 fallback 값을 사용합니다. |

병렬 실행 시 ID 충돌을 줄이기 위해 생성형 식별자는 UUID 기반 값을 우선 사용합니다.

```gherkin
* def uniqueId = java.util.UUID.randomUUID() + ''
```

## Negative case 생성 규칙

Negative case는 Swagger request schema에 명시된 제약만 기준으로 생성합니다.

| 기준 | 생성 규칙 | 기대 상태 |
|---|---|---|
| `required` | 필수 필드마다 하나씩 누락 요청을 만듭니다. | `400` |
| `enum` | request field에 `enum`이 있을 때 허용값 밖의 값을 넣습니다. | `400` |
| `format: date` | `not-a-date`를 넣습니다. | `400` |
| `format: date-time` | `not-a-date-time`을 넣습니다. | `400` |
| `format: uuid` | `not-a-uuid`를 넣습니다. | `400` |
| `format: email` | `not-an-email`을 넣습니다. | `400` |
| `minimum` | `minimum - 1` 값을 넣습니다. | `400` |
| `maximum` | `maximum + 1` 값을 넣습니다. | `400` |
| `minLength` | 허용 길이보다 짧은 문자열을 넣습니다. | `400` |
| `maxLength` | 허용 길이보다 긴 문자열을 넣습니다. | `400` |
| `pattern` | 정규식과 맞지 않는 문자열을 넣습니다. | `400` |

각 negative scenario는 한 번에 하나의 조건만 깨뜨립니다. 나머지 필드는 정상값을 유지해야 리포트에서 실패 원인이 명확해집니다.

response schema에만 있는 `enum`, `format`, `range`는 요청 negative case의 근거로 쓰지 않습니다. 요청에 들어갈 수 있는 field에 명시된 제약만 사용합니다.

## 실행 검증

서비스가 떠 있다면 생성 후 가장 좁은 범위로 Karate를 실행합니다.

```bash
cd karate-tests
ORG=orgB SERVICE=visit API=listSites ./gradlew test
```

전체 매트릭스 실행과 CATS 실행 방법은 [testing.md](testing.md)를 기준으로 확인합니다. CATS 리포트와 smoke/full 모드 상세 설명은 [cats-report-guide.md](cats-report-guide.md)에 분리되어 있습니다.
