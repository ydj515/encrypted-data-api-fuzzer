# Project Architecture

## Overview

This repository contains a local API proxy test harness and report viewer:

- **gateway** — receives plain-text requests from clients, encrypts them, forwards to an upstream API, then decrypts the response before returning it.
- **mock-rest-api-server** — simulates the upstream encrypted API, enabling fully local integration testing without a real backend.
- **karate-tests** — runs scenario-based E2E tests against the gateway.
- **report-server** — stores Karate/CATS execution history and serves report browsing UI.

## Module Structure

```
api-test-orchestrator/
├── gateway/                             # Encrypted API proxy (port 28080)
│   ├── src/main/java/com/example/gateway/
│   │   ├── controller/                  # HTTP entry points
│   │   ├── service/                     # Route forwarding, business logic
│   │   ├── config/                      # GatewayProperties, Spring config beans
│   │   ├── crypto/                      # CryptoModule / ChecksumModule interfaces + impls
│   │   ├── util/                        # Reusable header / URI helpers
│   │   └── exception/                   # RouteNotFoundException and global handler
│   ├── src/main/resources/
│   │   └── application-sample.yml       # Configuration template (no real secrets)
│   ├── libs/                            # Local JAR dependencies (Gradle fileTree)
│   └── docs/                            # gateway-specific documentation
│
├── mock-rest-api-server/                # Mock upstream API (port 18080)
│   ├── src/main/java/com/example/mockserver/
│   │   ├── controller/                  # Mock API endpoints
│   │   ├── service/                     # Mock response logic
│   │   ├── dto/                         # Request / response models
│   │   └── exception/                   # Error handling
│   ├── http/
│   │   └── mock-rest-api.http           # Manual request examples
│   └── docs/                            # mock-specific documentation
│
├── karate-tests/                        # Karate scenario tests
│   └── src/test/resources/scenarios/    # @service / @api tagged feature files
│
└── report-server/                       # Report UI and JSON APIs (port 48080)
    ├── src/main/java/com/example/reportserver/
    │   ├── controller/                  # Report pages and publish APIs
    │   ├── parser/                      # Karate/CATS report parsers
    │   └── service/                     # File storage, publish, query services
    └── data/runs/                       # Runtime report history (gitignored)
```

## Request Flow

```
Client
  │  plain-text body
  ▼
Gateway
  ├─ route lookup: (org, service, api, method) → GatewayProperties
  ├─ encrypt body       → CryptoModule.encrypt(key, body)
  ├─ compute checksum   → ChecksumModule.checksum(encrypted)
  ├─ set headers        → X-Checksum, X-Api-Key, X-Ins-Code, …
  └─ POST {"data": <encrypted>} to upstream
          │
          ▼
  Mock REST API Server  (or real upstream in production)
          │  {"data": <encrypted response>}
          ▼
Gateway
  ├─ extract $.data from response
  ├─ decrypt             → CryptoModule.decrypt(key, data)
  └─ return plain-text response to client
```

If decryption fails or `$.data` is absent, gateway logs a warning and returns the raw upstream response (facilitates debugging without masking upstream errors).

## Route Resolution

Routes are configured under `gateway.apis` in `application.yml`:

```yaml
gateway:
  apis:
    - org: acme
      service: payments
      api: charge
      method: POST
      host: http://localhost:18080
      externalPath: /api/payments/charge
      key: <encryption-key>
```

Lookup key: `(org, service, api, method)`. No match → `RouteNotFoundException` → HTTP 404.

## Crypto & Checksum Interfaces

```java
interface CryptoModule {
    String encrypt(String key, String plaintext);
    String decrypt(String key, String ciphertext);
}

interface ChecksumModule {
    String checksum(String data);
}
```

Local default implementations use Base64 encoding and SHA-256. Production implementations are injected via the `libs/` JAR or Spring profiles.

## Test And Report Flow

```
Karate / CATS
  │  call gateway through /cats/{org}/{service}/{api}
  ▼
Gateway
  │  encrypted upstream call
  ▼
Mock REST API Server
  │
  ▼
Karate / CATS raw reports
  │  publish scripts
  ▼
report-server/data/runs/{runId}/
  ├─ meta.json
  ├─ cases.json
  └─ report/
      └─ original HTML/JSON assets
```

The report server exposes:

- `/` — service summary
- `/services/{org}/{service}` — run history with filters
- `/runs/{runId}` — run detail and test case table
- `/api/services` and `/api/runs` — JSON APIs for automation
