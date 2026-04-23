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
| [`karate-tests/`](karate-tests/) | Karate scenario E2E tests | [karate-tests/AGENTS.md](karate-tests/AGENTS.md) |
| [`report-server/`](report-server/) | Test report web UI and JSON APIs (port 48080) | [report-server/AGENTS.md](report-server/AGENTS.md) |

## Browser Verification

- When changing `report-server` pages, templates, static assets, or web controllers, start the local report server and verify the affected UI with Playwright.
- Prefer the Playwright skill wrapper: `PWCLI="${CODEX_HOME:-$HOME/.codex}/skills/playwright/scripts/playwright_cli.sh"`.
- At minimum, open the changed route, take a snapshot, confirm the key text/actions render, navigate one primary link or submit the changed form, and check for visible error pages.
- Store any Playwright artifacts under `output/playwright/` if screenshots or traces are needed.
- If Playwright cannot run in the current environment, record the blocker and perform HTTP-level checks as a fallback.

## Quick Start

```shell
mise install           # install Java 21.0.2 (run once per machine)
mise run gateway:run   # start gateway service
mise run mock:run      # start mock server
mise run test          # run all tests sequentially
mise run build         # build all modules
```
