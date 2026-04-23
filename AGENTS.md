# api-test-orchestrator

Two Spring Boot services for testing encrypted API proxy flows.

## Table of Contents

| Topic | Document |
|---|---|
| Project structure & architecture | [docs/architecture.md](docs/architecture.md) |
| Runtime environment & mise tasks | [docs/development.md](docs/development.md) |
| Coding style & naming conventions | [docs/style-guide.md](docs/style-guide.md) |
| Testing guidelines | [docs/testing.md](docs/testing.md) |
| Commit & PR guidelines | [docs/contributing.md](docs/contributing.md) |
| Security & configuration | [docs/security.md](docs/security.md) |

## Modules

| Module | Description | Guidelines |
|---|---|---|
| [`gateway/`](gateway/) | Encrypted API proxy (port 8080) | [gateway/AGENTS.md](gateway/AGENTS.md) |
| [`mock-rest-api-server/`](mock-rest-api-server/) | Mock upstream API (port 18080) | [mock-rest-api-server/AGENTS.md](mock-rest-api-server/AGENTS.md) |

## Quick Start

```shell
mise install           # install Java 21.0.2 (run once per machine)
mise run gateway:run   # start gateway service
mise run mock:run      # start mock server
mise run test          # run all tests sequentially
mise run build         # build all modules
```
