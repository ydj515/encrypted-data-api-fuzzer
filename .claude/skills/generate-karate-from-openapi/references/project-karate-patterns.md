# Project Karate Patterns

## Scope

Use these rules for this repository only.

Repository Karate baseline:

- Dependency: `io.karatelabs:karate-junit5:1.5.2`
- Java imports: keep `com.intuit.karate.*` until Karate `1.6.x` changes package names
- `@setup` lifecycle is available and should be considered first for fixture-only setup in Karate `1.5.x`

Primary OpenAPI source:

```text
docs/openapi/mock-rest-api-server/
```

Primary Karate output root:

```text
karate-tests/src/test/resources/scenarios/
```

## Feature File Shape

Use this structure for generated files:

```gherkin
@service=<service> @api=<apiName> @kind=single-api
Feature: <org> <service> <purpose>

  Background:
    * url gatewayUrl
    * def org = '<org>'
    * def service = '<service>'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    Given path basePath + '/<apiName>'
    And request { ... }
    When method POST
    Then status 200
```

For legacy `booking` scenarios, some existing files rely on `karate-config.js` defaults. For generated org-specific files, prefer explicit `org`, `service`, and `basePath` in `Background`.

## Mode Decision

Single API mode:

- Use when the user asks for one endpoint, operation, or API contract.
- Output one folder per API: `scenarios/<service>/<api-kebab>/<api-kebab>.feature`.
- Include at least one success scenario.
- Add one required-field negative scenario per OpenAPI required request field.
- Add enum, format, and numeric range negative scenarios from OpenAPI request schemas when those constraints exist.
- If the API requires a created identifier, prefer Karate `@setup` or helper-style fixture preparation before inlining extra API calls into the main success scenario.
- If cleanup is required only to keep test data tidy, prefer a separate helper/setup strategy over turning the main success scenario into a lifecycle-style flow.

Scenario mode:

- Use when the user asks for a business flow, lifecycle, happy path, or multi-step test.
- Scope the scenario to exactly one OpenAPI document and one inferred `org/service`.
- Output one folder per flow: `scenarios/<service>/<scenario-kebab>/<scenario-kebab>.feature`.
- Use `--mode summary` and `--mode graph` for the same OpenAPI document to gather response-to-request candidates, then let Codex choose a semantically valid chain.
- Capture values from earlier responses and reuse them with `#(...)` only when the target request field has the same domain meaning.
- Avoid chaining fields that merely have the same primitive type or generic name unless OpenAPI operation descriptions, examples, or field names confirm the relationship.
- Do not chain APIs across different Swagger/OpenAPI documents or different services.
- Use a scenario-specific `@api` tag such as `reservationLifecycle`, `ticketLifecycle`, or `resourceScheduleReservationFlow` so 단건 API 리포트와 섞이지 않게 한다.
- Add `@kind=scenario` to scenario features.

## Plain OpenAPI to Gateway API Mapping

Karate tests call the gateway action path under `/cats/{org}/{service}/<apiName>`, while mock-rest-api-server OpenAPI files describe plain REST paths.

| Plain path suffix | Gateway API |
|---|---|
| `/resources` | `listResources` |
| `/resources/{resourceId}` | `getResourceDetail` |
| `/resources/{resourceId}/inventory` | `getResourceInventory` |
| `/resources/{resourceId}/schedules` | `getResourceSchedules` |
| `/schedules/daily` | `listDailySchedules` |
| `/reservations` | `createReservation` |
| `/reservations/{reservationId}` | `getReservationDetail` |
| `/reservations/{reservationId}/cancel` | `cancelReservation` |
| `/devices` | `listDevices` |
| `/tickets` | `createTicket` |
| `/tickets/{ticketId}` | `getTicketDetail` |
| `/tickets/{ticketId}/resolve` | `resolveTicket` |
| `/sites` | `listSites` |
| `/sites/{siteId}/slots` | `getSiteSlots` |
| `/visits` | `createVisit` |
| `/visits/{visitId}` | `getVisitDetail` |
| `/visits/{visitId}/cancel` | `cancelVisit` |

If a new path is not listed, infer the action from operation metadata in the selected OpenAPI document or from a matching gateway OpenAPI file under `docs/openapi/gateway/`.

## Stable Test Values

Prefer values from OpenAPI `example`, `examples`, `default`, and enum definitions. Use these repository fallback values only when the Swagger omits a sample:

| Field | Value |
|---|---|
| `resourceId` | `R-001` |
| `scheduleId` | `R-001-2026-02-26-9` |
| `reservationId` setup | capture from `createReservation` |
| `deviceId` | `DEV-01` |
| `ticketId` setup | capture from `createTicket` |
| `siteId` | `SITE-01` |
| `visitId` setup | capture from `createVisit` |
| `date`, `visitDate` | `2026-02-26` |
| `from` | `2026-02-26T09:00:00+09:00` |
| `to` | `2026-02-26T18:00:00+09:00` |
| `page` | `0` |
| `size` | `20` |
| `category` | `SPACE` |
| `status` | `ACTIVE` |
| `city` | `Seoul` |

## Validation Commands

Run focused Karate tests from `karate-tests`:

```bash
ORG=orgB SERVICE=support API=listDevices ./gradlew test
ORG=orgB SERVICE=visit API=createVisit ./gradlew test
ORG=orgA SERVICE=reservation API=createReservation ./gradlew test
```

The gateway and mock-rest-api-server must be running before these tests can pass.

## Review Checklist

- The feature path matches `scenarios/<service>/<case>/<case>.feature`.
- The first line includes `@service=<service>` and `@api=<apiName>`.
- Single API features use the real gateway action as `@api`; scenario features use a distinct scenario identifier as `@api`.
- Tag filters remain expressible as separate Karate tags instead of a single combined string expression.
- The gateway path uses `basePath + '/<apiName>'`.
- Path parameters from plain OpenAPI are present in the JSON request body.
- Response assertions are not only status checks.
- Every OpenAPI required request field has a matching missing-field negative scenario for single API tests.
- Request enum, format, and range constraints have matching negative scenarios only when the selected OpenAPI request schema defines those constraints.
- Scenario tests reuse captured values instead of hard-coded generated IDs.
- Negative tests assert the expected status and, when useful, the error response shape.

## Parallel Execution Note

`KarateRunner` uses `parallel(5)`. Generated entity IDs should be globally unique enough for parallel execution. Prefer UUID-based IDs with the existing domain prefix, such as `RSV-<uuid>`, `TCK-<uuid>`, or `VIS-<uuid>`.

When generating detail tests for resources with state enums, prefer enum-range assertions such as:

```gherkin
And match response.status == '#("CREATED" || "CANCELED")'
```

Use exact terminal assertions for the API that performs the terminal action itself, such as `cancelReservation`, `resolveTicket`, or `cancelVisit`.
