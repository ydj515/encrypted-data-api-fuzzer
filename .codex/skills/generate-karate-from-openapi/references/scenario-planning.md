# Scenario Planning

Use this reference when creating scenario-based Karate tests from one OpenAPI document.

## Goal

Build a scenario because the API chain makes business sense inside one service contract, not because paths happen to appear in a fixed order.

Scenario scope is exactly one selected Swagger/OpenAPI document. Do not combine APIs from multiple OpenAPI documents, multiple services, or multiple org/service path prefixes.

Good scenario shape:

```gherkin
Scenario: A에서 받은 값을 B 요청에 사용한다
  Given path basePath + '/apiA'
  And request { ... }
  When method POST
  Then status 201
  * def valueFromA = response.someField

  Given path basePath + '/apiB'
  And request { someField: '#(valueFromA)' }
  When method POST
  Then status 200
```

## Planning Steps

1. Select exactly one OpenAPI document.
2. Run `--mode summary` for that document.
3. Run `--mode graph` for that document to list same-name response-to-request candidates.
4. Use only OpenAPI summaries, descriptions, examples, field names, and schemas when a candidate is ambiguous. If the OpenAPI document does not justify the link, do not create that scenario.
5. Classify links:
   - High confidence: generated IDs such as `reservationId`, `ticketId`, `visitId` captured from create responses and used by detail/cancel/resolve APIs.
   - Medium confidence: filter values such as `status`, `category`, or `city` reused for list APIs. Use only when the API behavior clearly supports it.
   - Low confidence: generic text fields such as `description`, `memo`, `purpose`, or nullable fields. Do not chain them unless the domain logic explicitly requires it.
6. Choose the shortest scenario that proves a useful workflow.
7. Add cleanup when the scenario creates mutable state.

## Examples In This Repository

Reservation:

- `createReservation` produces `reservationId`.
- `getReservationDetail` consumes `reservationId`.
- `cancelReservation` consumes `reservationId`.
- Valid chain: create -> detail -> cancel.

Support:

- `createTicket` produces `ticketId`.
- `getTicketDetail` consumes `ticketId`.
- `resolveTicket` consumes `ticketId`.
- Valid chain: create -> detail -> resolve.

Visit:

- `createVisit` produces `visitId`.
- `getVisitDetail` consumes `visitId`.
- `cancelVisit` consumes `visitId`.
- Valid chain: create -> detail -> cancel.

## Rejection Rules

Do not create a scenario only because:

- Two fields have the same primitive type.
- A response field and request field share a generic name but not domain meaning.
- A field is nullable in the producer response and required by the consumer request.
- The chain leaves created data open when a terminal API exists.
- The scenario relies on non-unique generated IDs while running in parallel.
- The chain crosses OpenAPI document, org, or service boundaries.

## Output Rules

- Store scenario features under `karate-tests/src/test/resources/scenarios/<service>/<scenario-kebab>/<scenario-kebab>.feature`.
- Use `@service=<service> @api=<primaryApi>`.
- Name variables after domain values, for example `reservationId`, `ticketId`, `visitId`, or `ci`.
- Prefer exact assertions for values carried across API boundaries:

```gherkin
And match response.reservationId == reservationId
```
