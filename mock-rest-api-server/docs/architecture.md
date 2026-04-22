# mock-rest-api-server — Module Architecture

For the full system overview and how this service fits into the request flow, see [../../docs/architecture.md](../../docs/architecture.md).

## Purpose

Simulates the encrypted upstream API that gateway forwards requests to. Allows fully local end-to-end testing without a real backend.

## Package Layout

```
src/main/java/com/example/mockserver/
├── controller/   # @RestController — mock API endpoints matching gateway routes
├── service/      # Response generation logic
├── dto/          # Request / response model classes
└── exception/    # Error handling, global @ControllerAdvice
```

## Expected Request Format

The mock server receives requests from gateway in this shape:

```http
POST /api/<path>
X-Checksum: <checksum>
X-Api-Key: <key>
X-Ins-Code: <org>
Content-Type: application/json

{"data": "<encrypted body>"}
```

## Expected Response Format

```json
{"data": "<encrypted response>"}
```

Gateway will attempt to decrypt `$.data` from this response. If `data` is absent, gateway returns the raw response with a warning log.

## Manual Request Examples

See `http/mock-rest-api.http` for IntelliJ/VS Code HTTP client examples to test the mock server directly.
