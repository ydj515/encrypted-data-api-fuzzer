# Gateway — Testing Guide

For general testing philosophy and framework setup, see [../../docs/testing.md](../../docs/testing.md).

## Test Priorities

1. **Route resolution** (`GatewayPropertiesTest`) — correct match by `(org, service, api, method)`; missing route returns empty.
2. **Controller layer** (`GatewayControllerTest` with `@WebMvcTest`) — request dispatching, path variable extraction, HTTP status codes.
3. **Upstream call construction** — verify `X-Checksum`, `X-Api-Key`, `X-Ins-Code` headers and `{"data": ...}` body.
4. **Response decryption** — happy path (`$.data` present) and fallback (missing/malformed data).
5. **Exception mapping** — `RouteNotFoundException` → 404; unexpected errors → 500.

## Running Tests

```shell
# from gateway/
mise run test

# or directly
./gradlew test
```

## Mocking the Upstream

Use `MockRestServiceServer` (from `spring-test`) or WireMock to stub upstream HTTP calls:

```java
// MockRestServiceServer example
mockServer.expect(requestTo("http://localhost:18080/api/path"))
          .andExpect(method(HttpMethod.POST))
          .andExpect(header("X-Checksum", notNullValue()))
          .andRespond(withSuccess("{\"data\":\"<encrypted>\"}", MediaType.APPLICATION_JSON));
```

Do not rely on a live mock-rest-api-server in unit/slice tests — reserve live integration tests for a separate profile or manual verification.

## Test Data

Avoid real encryption keys in tests. Use the local Base64/SHA-256 stubs (default profile) so tests remain deterministic without crypto dependencies.
