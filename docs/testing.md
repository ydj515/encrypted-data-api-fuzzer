# Testing Guidelines

## Framework

`spring-boot-starter-test` (JUnit 5). No additional test runner required.

## Test Naming

| Pattern | Example |
|---|---|
| `<ClassName>Test` | `RouteResolverTest`, `GatewayControllerTest` |

## What to Test

Prioritize these areas when adding or changing behavior:

1. **Route resolution** — correct lookup by `(org, service, api, method)`; missing route → 404.
2. **Encryption/checksum flow** — `CryptoModule` and `ChecksumModule` are called with the right arguments.
3. **Upstream request construction** — correct headers (`X-Checksum`, `X-Api-Key`, etc.) and body `{"data": <encrypted>}`.
4. **Response decryption** — `$.data` extraction and decryption; fallback behavior on missing or malformed data.
5. **Exception response formatting** — HTTP status codes and response body shape for error cases.

## Slice vs. Integration Tests

- Use `@WebMvcTest` for controller-layer tests (fast, no full context).
- Use `@SpringBootTest` when testing the full wiring (config, crypto, routing together).
- Mock the upstream HTTP call (e.g., with WireMock or `MockRestServiceServer`) rather than hitting a live server in CI.

## Running Unit Tests

```shell
# gateway/mock/report-server unit tests from root
mise run unit:test

# single module
cd gateway && mise run test

# Gradle directly
cd gateway && ./gradlew test
```

Run tests before and after any behavior-changing commit. Include test output evidence in PR descriptions.

## Running Gateway E2E Tests

Start the mock server, gateway, and report server in separate terminals before running E2E tests:

```shell
mise run mock:run
mise run gateway:run
mise run report:run
```

From the repository root, `mise run test` runs Karate and/or CATS across all services in `docs/openapi/gateway/catalog.yaml` by default and publishes reports to `report-server/data/runs/`.
Publish 시 `report-server`는 `docs/openapi/gateway/catalog.yaml`과 선택된 Gateway OpenAPI 계약을 함께 읽어서 run meta의 `org/service/operationId/contract*` 값을 채웁니다.

```shell
# Karate + CATS (기본값)
mise run test

# Karate only
SOURCE=karate mise run test

# CATS only
SOURCE=cats mise run test

# 첫 실패에서 즉시 중단
FAIL_FAST=true mise run test

# 명시적인 전체 서비스 태스크
mise run test:all-services

# 기존 단일 서비스 실행 경로 유지
mise run test:single
```

Scope can be narrowed with `ORG`, `SERVICE`, and `API`. 필터를 주지 않으면 전체 서비스가 기본값입니다.

```shell
# 특정 기관 전체
ORG=orgB mise run test

# 특정 서비스 전체
ORG=orgB SERVICE=visit mise run test

# 특정 API만
ORG=orgB SERVICE=visit API=listSites SOURCE=cats mise run test
ORG=catsOrg SERVICE=booking API=createReservation SOURCE=karate mise run test
```

`SOURCE` defaults to `all`. CATS defaults to full mode. Use `CATS_PROFILE=smoke` for the smoke OpenAPI contract:

```shell
ORG=catsOrg SERVICE=booking API=listResources CATS_PROFILE=smoke SOURCE=cats mise run test
ORG=orgB SERVICE=visit API=listSites CATS_PROFILE=smoke SOURCE=cats mise run test
```

매트릭스 실행은 기본적으로 Gateway 계약 카탈로그의 전체 서비스를 순회합니다. `ORG`, `SERVICE`, `API`를 지정하면 해당 범위로만 좁혀집니다. CATS와 report publish 스크립트는 각 서비스별로 해당 Gateway 계약을 자동 선택합니다.

Shell script로도 같은 동작을 사용할 수 있습니다.

```shell
# 전체 서비스 기본 실행
./scripts/run-tests-matrix.sh

# 전체 서비스 대상 Karate만
SOURCE=karate ./scripts/run-tests-matrix.sh

# 특정 기관/서비스만
./scripts/run-tests-matrix.sh orgA reservation

# 특정 기관/서비스/API만
SOURCE=cats ./scripts/run-tests-matrix.sh orgB support listDevices

# 실제 실행 없이 대상 서비스와 명령 확인
DRY_RUN=true ./scripts/run-tests-matrix.sh

# 첫 실패에서 즉시 중단
FAIL_FAST=true ./scripts/run-tests-matrix.sh
```

매트릭스 스크립트는 실행 종료 시 아래 형식의 한 줄 요약도 출력합니다.

```text
[SUMMARY] source=all org=all service=all api=ALL total=4 success=4 failure=0 status=PASS fail_fast=false services=catsOrg/booking:PASS,orgA/reservation:PASS,orgB/visit:PASS,orgB/support:PASS
```

## OpenAPI 기반 Karate 생성

Karate feature 생성 규칙, 단건 API/시나리오 기반 생성 방식, Swagger 기준 negative case 규칙은 [karate-generation.md](karate-generation.md)를 기준으로 관리합니다.

CATS 리포트 해석과 smoke/full 실행 모드 상세 설명은 [cats-report-guide.md](cats-report-guide.md)에 분리되어 있습니다.

## 리포트 경로 정리

테스트를 돌릴 때 자주 헷갈리는 경로는 아래 세 가지입니다.

| 경로 | 생성 시점 | 역할 | `report-server` 연계 |
|---|---|---|---|
| `output/` | 수동 검증이나 디버깅 시 필요할 때 | 실행 로그, 요약 로그, Playwright 산출물 같은 임시 확인용 파일을 둡니다. | 직접 읽지 않습니다. 사람이 확인하는 용도입니다. |
| `cats-report/` | `SOURCE=cats` 또는 `SOURCE=all` 실행 중 CATS가 끝난 직후 | CATS CLI가 만든 가장 최근 raw 리포트입니다. HTML/JSON 원본이 여기에 생깁니다. | `scripts/publish-cats-report.sh`가 이 경로를 읽어 `report-server/data/runs/{runId}/report/`로 발행합니다. |
| `report-server/data/runs/` | publish 스크립트 실행 직후 | `report-server`가 사용하는 최종 run 저장소입니다. 각 run마다 `meta.json`, `cases.json`, `report/`가 들어 있습니다. | UI(`/`, `/services/{org}/{service}`, `/runs/{runId}`)와 JSON API가 이 경로를 기준으로 동작합니다. |

정리하면 `output/`은 로컬 확인용, `cats-report/`는 CATS raw 결과물, `report-server/data/runs/`는 발행 후 최종 저장소입니다.

참고로 `Karate` raw 리포트는 `karate-tests/build/karate-reports/karate-reports/` 아래에 생성됩니다. 이 경로는 `scripts/publish-karate-report.sh`가 읽어서 동일하게 `report-server/data/runs/{runId}/report/`로 복사합니다.

## Report UI

After publishing, open:

```text
http://localhost:48080/
```

- Service summary: `/`
- Run history: `/services/{org}/{service}`
- Run detail: `/runs/{runId}`
- Raw report assets: `report-server/data/runs/{runId}/report/`

서비스 홈(`/`)은 실행 이력만이 아니라 Gateway 계약 카탈로그에 정의된 서비스 전체를 보여줍니다. 아직 실행 이력이 없는 서비스는 마지막 수행/상태가 `-`로 표시됩니다.
