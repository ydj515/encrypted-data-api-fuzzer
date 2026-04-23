# mock-rest-api-server — Development Guide

For repository-wide setup (mise install, running both services together), see [../../docs/development.md](../../docs/development.md).

## Tasks (from `mock-rest-api-server/` directory)

```shell
mise run run     # start mock server on port 18080
mise run build   # compile, test, package
mise run test    # run JUnit 5 suite
mise run clean   # remove build artifacts
```

## Port

The mock server binds to port `18080` by default. Gateway is configured to forward requests to `http://localhost:18080`.

## Adding New Mock Endpoints

1. Add a route entry in `gateway/src/main/resources/application-local.yml` pointing to the mock server.
2. Implement the matching endpoint in `mock-rest-api-server/src/main/java/com/example/mockserver/controller/`.
3. Add a request example to `http/mock-rest-api.http`.
4. Add or update an integration test in `MockRestApiServerApplicationTests` (or an equivalent controller test).

## Manual Testing

Use the provided HTTP file for quick checks:

```
http/mock-rest-api.http   # IntelliJ HTTP Client / VS Code REST Client
```
