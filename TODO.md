# TODO — Karate 시나리오 테스트 & 리포트 서버 도입

## 목표

- **karate-tests**: 게이트웨이를 블랙박스로 두고 시나리오 단위 E2E 테스트 수행
- **report-server**: Karate + CATS 테스트 이력을 통합 조회할 수 있는 웹 UI 서빙
- **실행 범위 제어**: 서비스·API 단위로 CATS / Karate / 둘 다 선택 실행 가능

---

## 최종 모듈 구조

```
api-test-orchestrator/
├── gateway/                  # 기존
├── mock-rest-api-server/     # 기존
├── karate-tests/             # NEW
└── report-server/            # NEW
```

---

## 실행 범위 (Execution Scope) 설계

모든 실행 태스크는 아래 환경변수로 범위를 좁힐 수 있다.
지정하지 않으면 전체 대상을 실행한다.

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `ORG` | _(전체)_ | 실행 대상 org |
| `SERVICE` | _(전체)_ | 실행 대상 service |
| `API` | _(전체)_ | 실행 대상 api (단일 API 지정 시) |
| `SOURCE` | `all` | `karate` / `cats` / `all` |

### 사용 예시

```bash
# 전체 실행 (CATS + Karate)
mise run test

# 특정 서비스만, 둘 다
ORG=catsOrg SERVICE=booking mise run test

# 특정 API만, Karate만
ORG=catsOrg SERVICE=booking API=createReservation SOURCE=karate mise run test

# 특정 서비스, CATS만
ORG=catsOrg SERVICE=booking SOURCE=cats mise run test
```

### 범위 → 실행 도구 매핑

```
SOURCE=karate  → KarateRunner에 tag 필터 전달
                 (@service=booking @api=createReservation)
SOURCE=cats    → run-cats-gw*.sh에 --path 필터 전달
                 (/cats/{org}/{service}/createReservation)
SOURCE=all     → karate + cats 순차 실행
```

---

## 화면 명세

### 1. 서비스 목록 (`/`)

| 컬럼 | 설명 |
|---|---|
| 서비스명 | org + service 조합 |
| API 수 | 해당 서비스에 등록된 API endpoint 개수 |
| 마지막 수행 | 가장 최근 배치 실행 시각 |
| 상태 | 마지막 배치의 전체 pass/fail |
| 상세 보기 | 해당 서비스의 배치 이력 페이지로 이동 |

### 2. 배치 이력 (`/services/{org}/{service}`)

필터 조건:
- API 이름 (`api`)
- 테스트 상태 (`PASS` / `FAIL`)
- HTTP 상태 코드 (`200`, `400`, `404`, `500`, …)
- 테스트 케이스 명 (부분 문자열 검색)
- 엔드포인트 (부분 문자열 검색)
- 테스트 소스 (`KARATE` / `CATS`)
- 수행 시각 범위 (from ~ to)

목록 컬럼:

| 컬럼 | 설명 |
|---|---|
| 수행 시각 | 배치 실행 시작 시각 |
| 소스 | KARATE / CATS |
| 대상 API | 실행된 API (전체이면 "전체") |
| 전체 / 통과 / 실패 | 테스트 케이스 집계 |
| 소요 시간 | 전체 실행 시간 |
| 상세 레포트 | 원본 HTML 리포트 링크 |

### 3. 배치 상세 (`/runs/{runId}`)

- 배치 메타 요약: 서비스, 소스, 대상 API, 시각, 집계
- 테스트 케이스 테이블: 이름, 엔드포인트, HTTP 메서드, 상태, HTTP 상태 코드, 소요 시간
- Karate HTML 리포트 iframe 또는 새 탭 링크
- CATS 원본 HTML 리포트 링크

---

## 데이터 모델 (report-server)

```
TestRun
  id          : String (timestamp-based UUID)
  org         : String
  service     : String
  api         : String (nullable — null이면 서비스 전체 실행)
  source      : Enum(KARATE, CATS)
  startedAt   : LocalDateTime
  durationMs  : Long
  totalCount  : Int
  passCount   : Int
  failCount   : Int
  reportPath  : String  (서빙 경로)

TestCase
  id          : String
  runId       : String (FK → TestRun)
  api         : String (케이스가 속한 api 이름)
  name        : String
  endpoint    : String
  httpMethod  : String
  httpStatus  : Int
  status      : Enum(PASS, FAIL)
  durationMs  : Long
  failureMsg  : String (nullable)
```

저장소: 초기에는 파일시스템 JSON. 이력이 쌓이면 H2 → PostgreSQL 전환 고려.

```
report-server/data/runs/
├── {runId}/
│   ├── meta.json          # TestRun 메타데이터
│   ├── cases.json         # TestCase 목록
│   └── report/            # 원본 HTML 리포트 (정적 서빙)
│       ├── karate-*.html  # or cats-*.html
│       └── ...
```

---

## Phase 1 — karate-tests 모듈 셋업

- [ ] `karate-tests/` Gradle 모듈 생성
  - `build.gradle.kts`: `karate-junit5`, `junit-vintage-engine` 의존성
  - `mise.toml`: java 21.0.2
  - `AGENTS.md` + `CLAUDE.md` 심볼릭 링크

- [ ] feature 파일 디렉토리 구조 확정

  ```
  src/test/resources/
  ├── karate-config.js
  └── scenarios/
      └── {service}/
          └── {api}/
              └── *.feature    # @service=booking @api=createReservation 태그 필수
  ```

- [ ] `KarateRunner.java` 작성
  - 환경변수 `SERVICE`, `API` 로 Karate tag 필터 동적 구성
  - `SERVICE=booking API=createReservation` → `@service=booking and @api=createReservation`
  - 미지정 시 전체 feature 실행

  ```java
  // 태그 필터 조합 예시
  String tags = buildTagFilter(
      System.getenv("SERVICE"),   // @service=booking
      System.getenv("API")        // @api=createReservation
  );
  ```

- [ ] `karate-config.js` 작성
  - `GATEWAY_URL` (기본: `http://localhost:28080`)
  - `ORG` (기본: `catsOrg`), `SERVICE` (기본: `booking`)

- [ ] 첫 번째 feature 파일 작성

  ```gherkin
  # scenarios/booking/list-resources/list-resources.feature
  @service=booking @api=listResources
  Feature: 자원 목록 조회

  Scenario: 기본 목록 조회
    Given url gatewayUrl
    And path '/cats/' + org + '/booking/listResources'
    And request { page: 0, size: 20 }
    When method POST
    Then status 200
    And match response.items == '#array'
  ```

- [ ] 시나리오 체인 feature 작성

  ```gherkin
  # scenarios/booking/create-reservation/create-and-cancel.feature
  @service=booking @api=createReservation
  Feature: 예약 생성 → 상세 조회 → 취소 흐름

  Scenario: 예약 전체 생명주기
    # 1. 생성
    Given ...
    When method POST
    Then status 201
    And def reservationId = response.reservationId

    # 2. 상세 조회
    Given ...
    And request { reservationId: '#(reservationId)' }
    When method POST
    Then status 200

    # 3. 취소
    Given ...
    And request { reservationId: '#(reservationId)', reason: 'karate test' }
    When method POST
    Then status 200
    And match response.status == 'CANCELED'
  ```

- [ ] `karate-tests/` Gradle 실행 검증 (`./gradlew test`)

---

## Phase 2 — report-server 모듈 셋업

- [ ] `report-server/` Spring Boot 모듈 생성 (port: 48080)
  - `build.gradle.kts`: `spring-boot-starter-web`, `spring-boot-starter-thymeleaf`, `jackson-databind`
  - `mise.toml`: java 21.0.2
  - `AGENTS.md` + `CLAUDE.md` 심볼릭 링크
- [ ] `data/runs/` 디렉토리 구조 정의 및 `.gitignore` 처리 (`data/runs/` 전체 제외)
- [ ] `RunStorageService`: `meta.json` / `cases.json` 읽기·쓰기·목록 조회
- [ ] 정적 파일 서빙 설정 (`/reports/{runId}/**` → `data/runs/{runId}/report/`)
- [ ] `mise run report:run` 태스크 추가 (루트 `mise.toml`)

---

## Phase 3 — Karate 리포트 파싱 & 저장

- [ ] `KarateReportParser`: `karate-summary.json` 파싱 → `TestRun` 생성
  - `api` 필드: 환경변수 `API` 값 (없으면 null)
- [ ] `KarateCaseParser`: feature JSON 파싱 → `TestCase` 목록 생성
  - `api`: feature 태그에서 `@api=*` 추출
  - `endpoint`: 요청 URL에서 추출
  - HTTP 메서드, 상태 코드, pass/fail, duration
- [ ] `publish-karate-report.sh` 작성
  - 인자: `ORG`, `SERVICE`, `API` (환경변수 승계)
  - `karate-tests/build/karate-reports/` → `report-server/data/runs/{runId}/`
  - `meta.json`, `cases.json` 생성
- [ ] `mise run karate:publish` 태스크 추가
- [ ] `mise run karate` = `karate:run` + `karate:publish` 묶음

---

## Phase 4 — CATS 리포트 파싱 & 저장

- [ ] CATS JSON 리포트 포맷 분석 (`cats-report/` 디렉토리 기준)
- [ ] CATS 스크립트 범위 제어 추가
  - `API` 환경변수 지정 시 `--path /cats/{org}/{service}/{api}` 필터 적용
  - `run-cats-gw-full.sh`, `run-cats-gw-smoke.sh` 공통 처리
- [ ] `CatsReportParser`: CATS JSON → `TestRun` + `TestCase` 변환
  - `api`: 경로에서 마지막 세그먼트 추출
  - endpoint, HTTP 메서드, 상태 코드, fuzzing 케이스 이름, pass/fail
- [ ] `publish-cats-report.sh` 작성
- [ ] `mise run cats:publish` 태스크 추가
- [ ] `mise run cats` = `cats:run` + `cats:publish` 묶음

---

## Phase 5 — 서비스 목록 UI

- [ ] `ServiceSummaryService`: `data/runs/` 에서 org/service 단위로 집계
  - API 목록: 이력에서 등장한 api 값들 합산
- [ ] `ReportController.GET /` → 서비스 목록 모델
- [ ] `index.html` (Thymeleaf)
  - 서비스명, API 수, 마지막 수행 시각, 최근 상태 badge, 상세 보기 링크
- [ ] `GET /api/services` → JSON

---

## Phase 6 — 배치 이력 UI & 필터

- [ ] `RunQueryService`: 필터 조건으로 `TestRun` + `TestCase` 조회
  - `api`, `source`, `status`, `httpStatus`, `caseName`(부분일치), `endpoint`(부분일치), 시각 범위
- [ ] `ReportController.GET /services/{org}/{service}` → 이력 목록 + 필터 모델
- [ ] `history.html` (Thymeleaf)
  - 필터 폼: 드롭다운(source, status, httpStatus, api) + 텍스트(caseName, endpoint) + 날짜 범위
  - 이력 테이블: 수행 시각, 소스, 대상 API, 전체/통과/실패, 소요 시간, 상세 레포트 링크
- [ ] `GET /api/runs?org=&service=&api=&source=&status=&httpStatus=&…` → JSON

---

## Phase 7 — 배치 상세 UI

- [ ] `ReportController.GET /runs/{runId}` → 테스트 케이스 목록 모델
- [ ] `detail.html` (Thymeleaf)
  - 배치 메타 요약: 서비스, 소스, 대상 API, 시각, 집계
  - 테스트 케이스 테이블: api, 이름, 엔드포인트, 메서드, 상태, HTTP 상태 코드, 소요 시간
  - 원본 HTML 리포트 링크

---

## Phase 8 — 루트 mise 통합 & 문서

- [ ] 루트 `mise.toml` 전체 태스크 정리

  ```toml
  # --- 개별 실행 ---
  [tasks."karate:run"]
  description = "Karate 시나리오 실행 (ORG/SERVICE/API 환경변수로 범위 제한)"
  run = "scripts/run-karate.sh"

  [tasks."karate:publish"]
  description = "Karate 리포트를 report-server로 복사"
  run = "scripts/publish-karate-report.sh"

  [tasks."karate"]
  description = "Karate 실행 + 리포트 발행"
  run = """
  set -e
  mise run karate:run
  mise run karate:publish
  """

  [tasks."cats:run"]
  description = "CATS 퍼징 실행 (ORG/SERVICE/API 환경변수로 범위 제한)"
  run = "scripts/run-cats.sh"

  [tasks."cats:publish"]
  description = "CATS 리포트를 report-server로 복사"
  run = "scripts/publish-cats-report.sh"

  [tasks."cats"]
  description = "CATS 실행 + 리포트 발행"
  run = """
  set -e
  mise run cats:run
  mise run cats:publish
  """

  [tasks."test"]
  description = "전체 테스트 (SOURCE=karate|cats|all, ORG/SERVICE/API로 범위 제한)"
  run = "scripts/run-tests.sh"

  [tasks."report:run"]
  description = "report-server 기동 (port 48080)"
  run = "cd report-server && ./gradlew bootRun"
  ```

- [ ] `scripts/run-tests.sh` 작성: `SOURCE` 값에 따라 `karate`, `cats`, 또는 둘 다 실행
- [ ] `scripts/run-karate.sh` 작성: `ORG`/`SERVICE`/`API` → Karate tag 필터로 변환 후 Gradle 호출
- [ ] `scripts/run-cats.sh` 작성: `ORG`/`SERVICE`/`API` → CATS `--path` 필터로 변환 후 실행
- [ ] `docs/architecture.md` 업데이트
- [ ] 각 신규 모듈 `AGENTS.md` 작성
- [ ] `docs/testing.md` 업데이트 (실행 범위 제어 가이드 포함)

---

## 포트 정리

| 서비스 | 포트 |
|---|---|
| gateway | 28080 |
| mock-rest-api-server | 18080 |
| report-server | 48080 |

---

## 구현 순서 요약

```
Phase 1 → Phase 2 → Phase 3 → Phase 5 → Phase 6 → Phase 7 → Phase 4 → Phase 8
  Karate     report   Karate    서비스    배치 이력   배치 상세   CATS     mise +
  셋업       셋업     파싱      목록 UI   UI + 필터   UI         파싱     문서
```

CATS 파싱(Phase 4)은 Karate 흐름 완성 후 붙인다.
각 Phase는 독립적으로 동작 검증 후 다음 Phase로 진행한다.
