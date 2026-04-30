# Negative Case Generation

Use this reference when expanding single API Karate features from OpenAPI request constraints.

## Source Of Truth

OpenAPI/Swagger request definitions are the only source of truth for generated test cases. Do not inspect mock implementation code, controller code, service code, or seeded in-memory data to invent validation rules.

Use only:

- request path/query/body fields from the selected OpenAPI document
- request schema `required`, `enum`, `format`, `minimum`, `maximum`, `minLength`, `maxLength`, `pattern`, `example`, `examples`, and `default`
- operation summaries/descriptions for naming and scenario intent
- response fields from the same OpenAPI document for same-document data-flow scenarios

Response-only schemas do not create negative request tests.

## Generation Rules

Create negative scenarios only for one invalid condition at a time. Keep all other request fields valid so the failing condition is unambiguous.

Required:

- For every OpenAPI `required` request field, create one missing-field case.
- Scenario name: `OpenAPI required 필드 <field> 누락 시 400 반환`.
- Request body: same valid baseline request with only `<field>` omitted.

Enum:

- For every request field with `enum`, create one out-of-domain value case.
- Scenario name: `OpenAPI enum 필드 <field> 허용값 외 값 시 400 반환`.
- Invalid value: `__INVALID_ENUM__`.
- Do not use response enum values as request enum constraints.

Format:

- For `format: date`, use `not-a-date`.
- For `format: date-time`, use `not-a-date-time`.
- Scenario name: `OpenAPI format 필드 <field> 잘못된 <format> 형식 시 400 반환`.

Range:

- For numeric `minimum`, create a below-minimum case using `minimum - 1`.
- For numeric `maximum`, create an above-maximum case using `maximum + 1`.
- Scenario name:
  - `OpenAPI range 필드 <field> 최솟값 미만 시 400 반환`
  - `OpenAPI range 필드 <field> 최댓값 초과 시 400 반환`

String Length:

- For string `minLength`, create one below-minimum case.
- Prefer `""` when `minLength` is `1`; otherwise use a string with exactly `minLength - 1` characters.
- Scenario name: `OpenAPI minLength 필드 <field> 최소 길이 미만 시 400 반환`.

- For string `maxLength`, create one above-maximum case.
- Use a string with exactly `maxLength + 1` characters.
- Scenario name: `OpenAPI maxLength 필드 <field> 최대 길이 초과 시 400 반환`.

Pattern:

- For string `pattern`, create one non-matching case when a simple deterministic invalid value can be produced.
- Prefer a punctuation-only string such as `!` repeated to a valid length.
- Scenario name: `OpenAPI pattern 필드 <field> 패턴 위반 시 400 반환`.

## Output Rules

- Add negative cases to single API features, not LLM-planned business flow features.
- Keep setup/cleanup calls only when the target API requires generated IDs.
- Assert `Then status 400` for validation failures.
- Add error body assertions only when the error shape is stable and relevant to the test goal.
