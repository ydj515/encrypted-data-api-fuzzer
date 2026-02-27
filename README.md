# encrypted-data-api-fuzzer
this repository is encrypted-data-api-fuzzer

## CATS 실행 가이드 (smoke/full)

### 사전 준비
- `cats` CLI 설치
  - 예: `brew tap endava/tap && brew install cats`
- 서버 기동
  - Mock REST API Server: `mock-rest-api-server` (`18080`)
  - Gateway: `gateway` (`8080`)

```bash
cd /Users/dongjin/dev/study/encrypted-data-api-fuzzer/mock-rest-api-server
./gradlew bootRun
```

```bash
cd /Users/dongjin/dev/study/encrypted-data-api-fuzzer/gateway
./gradlew bootRun
```

### Smoke 모드 (짧은 실행)
- 축약 계약 파일: `docs/openapi/cats-gw-smoke-openapi.yaml`

```bash
/Users/dongjin/dev/study/encrypted-data-api-fuzzer/scripts/run-cats-gw-smoke.sh
```

### Full 모드 (전체 실행)
- 전체 계약 파일: `docs/openapi/cats-gw-openapi.yaml`

```bash
/Users/dongjin/dev/study/encrypted-data-api-fuzzer/scripts/run-cats-gw-full.sh
```

### 기존 스크립트 호환
- `scripts/run-cats-gw.sh`는 full 모드 래퍼입니다.

```bash
/Users/dongjin/dev/study/encrypted-data-api-fuzzer/scripts/run-cats-gw.sh
```

### 환경변수 오버라이드
- 공통 환경변수: `CONTRACT_PATH`, `SERVER_URL`, `ORG`, `SERVICE`, `CATS_BIN`, `DRY_RUN`

```bash
ORG=testOrg SERVICE=testService \
/Users/dongjin/dev/study/encrypted-data-api-fuzzer/scripts/run-cats-gw-smoke.sh
```

```bash
DRY_RUN=true CATS_BIN=echo \
/Users/dongjin/dev/study/encrypted-data-api-fuzzer/scripts/run-cats-gw-full.sh
```
