# mock-rest-api-server

Spring Boot application simulating an upstream encrypted API for local development and testing.

## Table of Contents

| Topic | Document |
|---|---|
| Module structure & architecture | [docs/architecture.md](docs/architecture.md) |
| Runtime environment & build tasks | [docs/development.md](docs/development.md) |
| Testing guidelines | [docs/testing.md](docs/testing.md) |
| Coding style (repository-wide) | [../docs/style-guide.md](../docs/style-guide.md) |
| Commit & PR guidelines | [../docs/contributing.md](../docs/contributing.md) |
| Security & configuration | [../docs/security.md](../docs/security.md) |

## Quick Start

```shell
mise install       # install Java 21.0.2
mise run run       # start mock server (port 18080)
mise run test      # run tests
```
