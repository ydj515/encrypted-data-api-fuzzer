# Gateway — Module Architecture

For the full system overview and request flow, see [../../docs/architecture.md](../../docs/architecture.md).

## Package Layout

```
src/main/java/com/example/gateway/
├── controller/       # @RestController — receives client requests, delegates to service
├── service/          # Route forwarding, upstream HTTP calls, response handling
├── config/           # GatewayProperties (@ConfigurationProperties), Spring beans
├── crypto/           # CryptoModule + ChecksumModule interfaces and local stubs
├── util/             # Header builders, URI helpers
└── exception/        # RouteNotFoundException, global @ControllerAdvice
```

## Key Classes

| Class | Responsibility |
|---|---|
| `GatewayController` | Entry point: maps `/{org}/{service}/{api}` to service call |
| `GatewayService` | Orchestrates route lookup, encryption, upstream call, decryption |
| `GatewayProperties` | Binds `gateway.apis[]` config; provides route lookup |
| `CryptoModule` | Interface: `encrypt(key, plain)` / `decrypt(key, cipher)` |
| `ChecksumModule` | Interface: `checksum(data)` |
| `RouteNotFoundException` | Thrown when `(org, service, api, method)` has no matching route |

## Configuration Shape

```yaml
gateway:
  apis:
    - org: <institution>
      service: <service-name>
      api: <api-name>
      method: POST
      host: http://localhost:18080
      externalPath: /api/path
      key: <crypto-key>
```

## Upstream Request Format

```http
POST <host><externalPath>
X-Checksum: <sha256 of encrypted body>
X-Api-Key: <key>
X-Ins-Code: <org>
Content-Type: application/json

{"data": "<encrypted body>"}
```

## Error Handling

| Scenario | Behavior |
|---|---|
| Route not found | 404 via `RouteNotFoundException` |
| Upstream HTTP error | Propagate status code; log at WARN |
| Decryption failure / missing `$.data` | Log warning; return raw upstream response |
| Malformed request body | 400 |
