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

## Running Tests

```shell
# all modules (from root)
mise run test

# single module
cd gateway && mise run test

# Gradle directly
cd gateway && ./gradlew test
```

Run tests before and after any behavior-changing commit. Include test output evidence in PR descriptions.
