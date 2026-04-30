# karate-tests

Karate 기반 E2E 시나리오 테스트 모듈. 게이트웨이를 블랙박스로 두고 실제 HTTP 흐름을 검증한다.

## Table of Contents

| Topic | Document |
|---|---|
| 모듈 구조 & 시나리오 설계 | [docs/architecture.md](docs/architecture.md) |
| 실행 방법 & 환경 설정 | [docs/development.md](docs/development.md) |
| 테스트 작성 가이드 | [docs/testing.md](docs/testing.md) |
| 코딩 스타일 (공통) | [../docs/style-guide.md](../docs/style-guide.md) |
| Commit & PR 가이드라인 | [../docs/contributing.md](../docs/contributing.md) |

## Quick Start

```shell
mise install                                             # Java 21.0.2 설치
mise run run                                             # 전체 시나리오 실행
ORG=orgA SERVICE=reservation API=listResources mise run run  # 특정 API만 실행
```

> gateway(28080)와 mock-rest-api-server(18080)가 먼저 기동되어 있어야 한다.
