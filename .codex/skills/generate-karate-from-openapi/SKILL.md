---
name: generate-karate-from-openapi
description: Generate Karate feature tests for this api-test-orchestrator workspace strictly from OpenAPI/Swagger YAML documents, especially docs/openapi/mock-rest-api-server/*.yaml. Use when Codex needs to create or update Karate tests under karate-tests/src/test/resources/scenarios in single API mode or LLM-planned scenario mode scoped to one Swagger/OpenAPI document and one org/service, infer org/service/api names from OpenAPI paths and operation metadata, scaffold request bodies and response assertions, discover response-to-request data-flow candidates within that one document, or align new Karate tests with this repository's gateway test conventions. Do not infer test inputs, validation rules, or scenarios from mock implementation code; keep Swagger and the current REST API spec as the source of truth.
---

# Generate Karate From OpenAPI

> **싱크 안내:** 이 파일을 수정한 후에는 `docs/skills/generate-karate-from-openapi/SKILL-SYNC.md`를 참조하여 `.claude/skills/generate-karate-from-openapi/`의 대응 항목도 함께 업데이트하세요.

## Overview

Create Karate tests from mock REST OpenAPI specs while preserving this repository's conventions: gateway-style POST calls, `@service` and `@api` tags, and files under `karate-tests/src/test/resources/scenarios`.

For scenario-based tests, do not merely emit a fixed lifecycle template. Scope the scenario to one selected Swagger/OpenAPI document, which corresponds to one org/service. Inspect only OpenAPI paths, request fields, response fields, operation summaries/descriptions, examples, defaults, and existing Karate style, then choose an API chain where an earlier response value is semantically valid input for a later request.

## Quick Start

From the repository root:

```bash
python3 .codex/skills/generate-karate-from-openapi/scripts/generate_karate_features.py \
  --openapi docs/openapi/mock-rest-api-server/orgB-support-plain-api.yaml \
  --mode summary
```

Generate single API tests:

```bash
python3 .codex/skills/generate-karate-from-openapi/scripts/generate_karate_features.py \
  --openapi docs/openapi/mock-rest-api-server/orgB-support-plain-api.yaml \
  --mode api \
  --api listDevices
```

Inspect data-flow candidates before designing a scenario:

```bash
python3 .codex/skills/generate-karate-from-openapi/scripts/generate_karate_features.py \
  --openapi docs/openapi/mock-rest-api-server/orgB-visit-plain-api.yaml \
  --mode graph
```

Use `--dry-run` before writing if you need to inspect generated feature text. Use `--skip-existing` when generating missing single API files in bulk. Use `--overwrite` only after reading the existing test and confirming replacement is intended.

## Workflow

1. Read `karate-tests/AGENTS.md`, `karate-tests/src/test/resources/karate-config.js`, and nearby `.feature` files for style.
2. Inspect the target OpenAPI YAML from `docs/openapi/mock-rest-api-server/`.
3. Run the generator in `--mode summary` to list inferred `org`, `service`, gateway API names, request fields, and response fields.
4. Choose generation mode:
   - Single API: use `--mode api --api <apiName>` or omit `--api` to scaffold all inferred APIs. Add `--skip-existing` for missing-only generation.
   - Scenario: use `--mode graph` on one selected OpenAPI document to find response-to-request candidates, then read `references/scenario-planning.md` and compose the scenario with LLM judgment. Use `--mode scenario` only as a conservative bootstrap for already recognized create-detail-terminal flows within that same document.
5. Review generated files against the same OpenAPI document and nearby `.feature` style. For scenario tests, explicitly capture response values with `* def` and feed them into later request bodies with `#(...)`.
6. Run a focused Karate test when services are available:

```bash
cd karate-tests
ORG=<org> SERVICE=<service> API=<apiName> ./gradlew test
```

When upstream services are unavailable, still validate the generated feature syntax by reading it against existing Karate patterns.

## Repository Conventions

Load `references/project-karate-patterns.md` when you need path, tag, naming, API mapping, or validation details.

Load `references/scenario-planning.md` before creating scenario-based tests that require reasoning about API chains or response-value reuse.

Load `references/negative-case-generation.md` before expanding single API tests with required, enum, format, or range validation cases.

Core rules:

- Write features under `karate-tests/src/test/resources/scenarios/<service>/<case-name>/<case-name>.feature`.
- Use tags like `@service=support @api=createTicket` so `KarateRunner` can filter by `SERVICE` and `API`.
- Set `Background` with `url gatewayUrl`, fixed `org`, fixed `service`, and `basePath = '/cats/' + org + '/' + service`.
- Call the gateway action path, not the plain mock-rest-api path: `Given path basePath + '/createTicket'`.
- Use POST with a JSON request body for gateway tests, including path variables from the plain API as request fields.
- Prefer OpenAPI `example`, `examples`, `default`, and enum values for request samples. Use deterministic schema-valid fallback values only when the Swagger omits examples.
- For scenario tests, capture meaningful response values and reuse them through `#(...)` only when the next API's request field has the same domain meaning.
- Do not build scenario tests by chaining APIs across different Swagger/OpenAPI documents or different services.

## Script Notes

`scripts/generate_karate_features.py` requires Python 3 and PyYAML. If PyYAML is missing, install or use the script as a readable template and create the feature manually.

The script deliberately produces a conservative scaffold. Strengthen it after generation with:

- one missing-field negative scenario per OpenAPI required request field
- enum, format, and range negative scenarios from OpenAPI request schemas
- exact status transitions such as `OPEN`, `RESOLVED`, `REQUESTED`, `CANCELED`, or `CREATED`
- aggregate assertions such as `totalQuantity == availableQuantity + reservedQuantity`
- setup calls for single APIs that need generated IDs

For single API tests, the script can add setup and cleanup calls when a required request ID is produced by another API. Treat the file as a single API test when only one API is the target under test and other calls exist solely for setup or cleanup.

## Validation

Run the skill validator after changing this skill:

```bash
python3 "${CODEX_HOME:-$HOME/.codex}/skills/.system/skill-creator/scripts/quick_validate.py" \
  .codex/skills/generate-karate-from-openapi
```

Validate generated tests with the narrowest useful scope:

```bash
cd karate-tests
ORG=orgB SERVICE=support API=listDevices ./gradlew test
```
