# mock-rest-api-server — Testing Guide

For general testing philosophy and framework setup, see [../../docs/testing.md](../../docs/testing.md).

## Test Priorities

1. **Endpoint happy paths** — each mock endpoint returns the expected `{"data": "..."}` structure.
2. **Request validation** — required headers (`X-Checksum`, `X-Api-Key`, etc.) absent or malformed → appropriate error response.
3. **Error scenarios** — verify HTTP status codes (400, 404, 500) and response body shape.
4. **Service-layer branching** — any conditional logic in mock service classes.

## Running Tests

```shell
# from mock-rest-api-server/
mise run test

# or directly
./gradlew test
```

## Notes

Since this is a mock server, tests should focus on the contract it presents to gateway — not on simulating complex business logic. Keep test coverage lean and targeted at the HTTP interface.
