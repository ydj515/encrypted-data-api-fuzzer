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

From the repository root, `mise run test` runs Karate and/or CATS and publishes reports to `report-server/data/runs/`.

```shell
# Karate + CATS
SOURCE=all mise run test

# Karate only
SOURCE=karate mise run test

# CATS only
SOURCE=cats mise run test
```

Scope can be narrowed with `ORG`, `SERVICE`, and `API`.

```shell
ORG=catsOrg SERVICE=booking API=createReservation SOURCE=karate mise run test
ORG=catsOrg SERVICE=booking API=listResources SOURCE=cats mise run test
```

`SOURCE` defaults to `all`. CATS defaults to full mode. Use `CATS_PROFILE=smoke` for the smoke OpenAPI contract:

```shell
ORG=catsOrg SERVICE=booking API=listResources CATS_PROFILE=smoke SOURCE=cats mise run test
```

## Report UI

After publishing, open:

```text
http://localhost:48080/
```

- Service summary: `/`
- Run history: `/services/{org}/{service}`
- Run detail: `/runs/{runId}`
- Raw report assets: `report-server/data/runs/{runId}/report/`
