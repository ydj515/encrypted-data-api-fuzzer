#!/usr/bin/env python3
"""OpenAPI YAML에서 이 저장소용 Karate feature 초안을 생성한다."""

from __future__ import annotations

import argparse
import datetime as dt
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any

try:
    import yaml
except ImportError as exc:  # pragma: no cover - 환경 안내용 분기
    raise SystemExit("PyYAML이 필요합니다. python3 -m pip install pyyaml 후 다시 실행하세요.") from exc


HTTP_METHODS = {"get", "post", "put", "patch", "delete"}

PATH_TO_API = {
    "/resources": "listResources",
    "/resources/{resourceId}": "getResourceDetail",
    "/resources/{resourceId}/inventory": "getResourceInventory",
    "/resources/{resourceId}/schedules": "getResourceSchedules",
    "/schedules/daily": "listDailySchedules",
    "/reservations": "createReservation",
    "/reservations/{reservationId}": "getReservationDetail",
    "/reservations/{reservationId}/cancel": "cancelReservation",
    "/devices": "listDevices",
    "/tickets": "createTicket",
    "/tickets/{ticketId}": "getTicketDetail",
    "/tickets/{ticketId}/resolve": "resolveTicket",
    "/sites": "listSites",
    "/sites/{siteId}/slots": "getSiteSlots",
    "/visits": "createVisit",
    "/visits/{visitId}": "getVisitDetail",
    "/visits/{visitId}/cancel": "cancelVisit",
}

SAMPLE_VALUES = {
    "resourceId": "R-001",
    "scheduleId": "R-001-2026-02-26-9",
    "reservationId": "RSV-NONEXISTENT-999",
    "deviceId": "DEV-01",
    "ticketId": "TCK-NONEXISTENT-999",
    "siteId": "SITE-01",
    "visitId": "VIS-NONEXISTENT-999",
    "date": "2026-02-26",
    "visitDate": "2026-02-26",
    "from": "2026-02-26T09:00:00+09:00",
    "to": "2026-02-26T18:00:00+09:00",
    "page": 0,
    "size": 20,
    "category": "SPACE",
    "status": "ACTIVE",
    "city": "Seoul",
    "userId": "user-karate-001",
    "requesterId": "ops-user",
    "issueType": "DISPLAY_ERROR",
    "description": "screen is blank",
    "resolutionNote": "restarted device",
    "quantity": 1,
    "memo": "karate generated test",
    "reason": "karate generated teardown",
    "visitorName": "Kim Visitor",
    "visitorPhone": "010-1234-5678",
    "partySize": 2,
    "purpose": "demo",
}

STABLE_ID_FIELDS = {"resourceId", "scheduleId", "deviceId", "siteId", "userId", "requesterId"}
CREATE_API_PREFIXES = ("create", "register", "issue", "open", "submit")
TERMINAL_API_PREFIXES = ("cancel", "resolve", "delete", "close", "complete")
# 생성된 feature가 과도하게 길어지지 않도록 기본 응답 검증 필드 수를 제한한다.
MAX_RESPONSE_ASSERTION_FIELDS = 8

LIFECYCLES = {
    "reservation": {
        "name": "reservation-lifecycle",
        "feature": "orgA 예약 서비스 예약 전체 생명주기",
        "scenario_api": "reservationLifecycle",
        "id_field": "reservationId",
        "id_var": "reservationId",
        "steps": ["createReservation", "getReservationDetail", "cancelReservation"],
        "status": {
            "createReservation": "CREATED",
            "getReservationDetail": "CREATED",
            "cancelReservation": "CANCELED",
        },
    },
    "support": {
        "name": "ticket-lifecycle",
        "feature": "orgB support 티켓 생명주기",
        "scenario_api": "ticketLifecycle",
        "id_field": "ticketId",
        "id_var": "ticketId",
        "steps": ["createTicket", "getTicketDetail", "resolveTicket"],
        "status": {
            "createTicket": "OPEN",
            "getTicketDetail": "OPEN",
            "resolveTicket": "RESOLVED",
        },
    },
    "visit": {
        "name": "visit-lifecycle",
        "feature": "orgB visit 예약 생명주기",
        "scenario_api": "visitLifecycle",
        "id_field": "visitId",
        "id_var": "visitId",
        "steps": ["createVisit", "getVisitDetail", "cancelVisit"],
        "status": {
            "createVisit": "REQUESTED",
            "getVisitDetail": "REQUESTED",
            "cancelVisit": "CANCELED",
        },
    },
}


@dataclass(frozen=True)
class Field:
    name: str
    schema: dict[str, Any]
    required: bool
    source: str


@dataclass(frozen=True)
class Operation:
    api: str
    method: str
    path: str
    summary: str
    operation_id: str
    success_status: int
    request_fields: list[Field]
    response_schema: dict[str, Any]


@dataclass(frozen=True)
class Link:
    producer: Operation
    consumer: Operation
    field_name: str
    reason: str


def load_spec(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        loaded = yaml.safe_load(handle)
    if not isinstance(loaded, dict):
        raise SystemExit(f"OpenAPI 문서를 읽을 수 없습니다: {path}")
    return loaded


def resolve_ref(spec: dict[str, Any], value: Any) -> Any:
    if not isinstance(value, dict) or "$ref" not in value:
        return value
    ref = value["$ref"]
    if not ref.startswith("#/"):
        raise SystemExit(f"외부 $ref는 지원하지 않습니다: {ref}")
    current: Any = spec
    for part in ref[2:].split("/"):
        decoded_part = part.replace("~1", "/").replace("~0", "~")
        if isinstance(current, dict):
            if decoded_part not in current:
                raise SystemExit(f"잘못된 내부 $ref 경로입니다: {ref} (누락된 키: {decoded_part})")
            current = current[decoded_part]
            continue
        if isinstance(current, list):
            try:
                current = current[int(decoded_part)]
            except (ValueError, IndexError) as exc:
                raise SystemExit(f"잘못된 내부 $ref 경로입니다: {ref} (잘못된 배열 인덱스: {decoded_part})") from exc
            continue
        raise SystemExit(f"잘못된 내부 $ref 경로입니다: {ref} ({decoded_part} 탐색 불가)")
    return resolve_ref(spec, current)


def expand_schema(spec: dict[str, Any], schema: Any) -> dict[str, Any]:
    schema = resolve_ref(spec, schema)
    if not isinstance(schema, dict):
        return {}
    if "allOf" not in schema:
        return schema

    merged: dict[str, Any] = {key: value for key, value in schema.items() if key != "allOf"}
    properties: dict[str, Any] = {}
    required: list[str] = []
    for item in schema.get("allOf", []):
        expanded = expand_schema(spec, item)
        properties.update(expanded.get("properties", {}))
        required.extend(expanded.get("required", []))

    own_properties = merged.get("properties", {})
    if isinstance(own_properties, dict):
        properties.update(own_properties)
    own_required = merged.get("required", [])
    if isinstance(own_required, list):
        required.extend(own_required)

    merged["type"] = merged.get("type", "object")
    if properties:
        merged["properties"] = properties
    if required:
        merged["required"] = list(dict.fromkeys(required))
    return merged


def extract_org_service(spec: dict[str, Any]) -> tuple[str, str]:
    for raw_path in spec.get("paths", {}):
        match = re.match(r"^/cats/([^/]+)/([^/]+)(?:/|$)", raw_path)
        if match:
            return match.group(1), match.group(2)
    raise SystemExit("OpenAPI paths에서 /cats/{org}/{service} 패턴을 찾지 못했습니다.")


def suffix_path(path: str, org: str, service: str) -> str:
    prefix = f"/cats/{org}/{service}"
    if path.startswith(prefix):
        return path[len(prefix) :] or "/"
    return path


def infer_api_name(path: str, method: str, operation: dict[str, Any], org: str, service: str) -> str:
    suffix = suffix_path(path, org, service)
    if suffix in PATH_TO_API:
        return PATH_TO_API[suffix]

    operation_id = operation.get("operationId") or ""
    cleaned = re.sub(r"ViaGateway$", "", operation_id)
    cleaned = re.sub(r"ByBody$", "", cleaned)
    cleaned = re.sub(rf"^{re.escape(org)}", "", cleaned, flags=re.IGNORECASE)
    cleaned = re.sub(rf"^{re.escape(service)}", "", cleaned, flags=re.IGNORECASE)
    if cleaned:
        return cleaned[0].lower() + cleaned[1:]
    return method.lower() + "".join(part.capitalize() for part in suffix.strip("/").split("/"))


def json_schema(operation: dict[str, Any], *keys: str) -> dict[str, Any]:
    current: Any = operation
    for key in keys:
        if not isinstance(current, dict):
            return {}
        current = current.get(key)
    return current if isinstance(current, dict) else {}


def request_body_schema(spec: dict[str, Any], operation: dict[str, Any]) -> dict[str, Any]:
    content = json_schema(operation, "requestBody", "content", "application/json")
    return expand_schema(spec, content.get("schema", {})) if content else {}


def response_schema(spec: dict[str, Any], operation: dict[str, Any]) -> tuple[int, dict[str, Any]]:
    responses = operation.get("responses", {})
    for code in sorted(responses, key=str):
        if not str(code).startswith("2"):
            continue
        response = resolve_ref(spec, responses[code])
        content = json_schema(response, "content", "application/json")
        return int(code), expand_schema(spec, content.get("schema", {})) if content else {}
    return 200, {}


def collect_parameters(spec: dict[str, Any], operation: dict[str, Any]) -> list[Field]:
    fields: list[Field] = []
    for raw_param in operation.get("parameters", []):
        param = resolve_ref(spec, raw_param)
        if not isinstance(param, dict):
            continue
        name = param.get("name")
        if not name:
            continue
        schema = resolve_ref(spec, param.get("schema", {}))
        if isinstance(schema, dict):
            schema = dict(schema)
            for key in ("example", "examples", "default"):
                if key in param and key not in schema:
                    schema[key] = param[key]
        fields.append(
            Field(
                name=name,
                schema=schema,
                required=bool(param.get("required")),
                source=str(param.get("in", "parameter")),
            )
        )
    return fields


def collect_body_fields(spec: dict[str, Any], schema: dict[str, Any]) -> list[Field]:
    schema = expand_schema(spec, schema)
    required = set(schema.get("required", []))
    properties = schema.get("properties", {})
    if not isinstance(properties, dict):
        return []
    return [
        Field(name=name, schema=resolve_ref(spec, prop), required=name in required, source="body")
        for name, prop in properties.items()
    ]


def collect_operations(spec: dict[str, Any]) -> tuple[str, str, list[Operation]]:
    org, service = extract_org_service(spec)
    operations: list[Operation] = []
    for path, path_item in spec.get("paths", {}).items():
        if not isinstance(path_item, dict):
            continue
        for method, operation in path_item.items():
            if method.lower() not in HTTP_METHODS or not isinstance(operation, dict):
                continue
            body_schema = request_body_schema(spec, operation)
            fields = collect_parameters(spec, operation)
            fields.extend(collect_body_fields(spec, body_schema))
            fields = dedupe_fields(fields)
            status, resp_schema = response_schema(spec, operation)
            operations.append(
                Operation(
                    api=infer_api_name(path, method, operation, org, service),
                    method=method.upper(),
                    path=path,
                    summary=str(operation.get("summary", "")),
                    operation_id=str(operation.get("operationId", "")),
                    success_status=status,
                    request_fields=fields,
                    response_schema=resp_schema,
                )
            )
    return org, service, prefer_gateway_operations(operations)


def dedupe_fields(fields: list[Field]) -> list[Field]:
    result: list[Field] = []
    seen: set[str] = set()
    for field in fields:
        if field.name in seen:
            continue
        seen.add(field.name)
        result.append(field)
    return result


def prefer_gateway_operations(operations: list[Operation]) -> list[Operation]:
    by_api: dict[str, list[Operation]] = {}
    for operation in operations:
        by_api.setdefault(operation.api, []).append(operation)

    preferred: list[Operation] = []
    for api, candidates in by_api.items():
        candidates.sort(
            key=lambda op: (
                0 if op.method == "POST" else 1,
                0 if op.request_fields else 1,
                op.path,
            )
        )
        preferred.append(candidates[0])
    return sorted(preferred, key=lambda op: op.api)


def kebab(value: str) -> str:
    value = re.sub(r"([a-z0-9])([A-Z])", r"\1-\2", value)
    value = re.sub(r"[^A-Za-z0-9]+", "-", value)
    return value.strip("-").lower()


def karate_value(value: Any) -> str:
    if value is None:
        return "null"
    if isinstance(value, (dt.datetime, dt.date)):
        return "'" + value.isoformat() + "'"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, list):
        return "[" + ", ".join(karate_value(item) for item in value) + "]"
    if isinstance(value, dict):
        return "{ " + ", ".join(f"{key}: {karate_value(val)}" for key, val in value.items()) + " }"
    return "'" + str(value).replace("'", "\\'") + "'"


def sample_for(field: Field) -> Any:
    schema = field.schema
    if "example" in schema:
        return schema["example"]
    if "default" in schema:
        return schema["default"]
    if field.name in SAMPLE_VALUES:
        return SAMPLE_VALUES[field.name]
    enum = schema.get("enum")
    if isinstance(enum, list) and enum:
        return enum[0]

    schema_type = schema.get("type")
    schema_format = schema.get("format")
    lowered = field.name.lower()
    if schema_format == "date":
        return "2026-02-26"
    if schema_format == "date-time":
        return "2026-02-26T09:00:00+09:00"
    if schema_type in {"integer", "number"}:
        return 1
    if schema_type == "boolean":
        return True
    if schema_type == "array":
        return []
    if "phone" in lowered:
        return "010-1234-5678"
    if lowered.endswith("id"):
        return field.name.upper() + "-001"
    return "karate-generated"


def request_object(fields: list[Field], omit: str | None = None, overrides: dict[str, Any] | None = None) -> str:
    overrides = overrides or {}
    entries = []
    for field in fields:
        if field.name == omit:
            continue
        value = overrides[field.name] if field.name in overrides else sample_for(field)
        entries.append(f"{field.name}: {karate_value(value)}")
    return "{ " + ", ".join(entries) + " }" if entries else "{ }"


def invalid_constraint_cases(field: Field) -> list[tuple[str, Any]]:
    schema = field.schema
    cases: list[tuple[str, Any]] = []

    enum = schema.get("enum")
    if isinstance(enum, list) and enum:
        cases.append((f"OpenAPI enum 필드 {field.name} 허용값 외 값 시 400 반환", "__INVALID_ENUM__"))

    schema_format = schema.get("format")
    if schema_format == "date":
        cases.append((f"OpenAPI format 필드 {field.name} 잘못된 date 형식 시 400 반환", "not-a-date"))
    elif schema_format == "date-time":
        cases.append((f"OpenAPI format 필드 {field.name} 잘못된 date-time 형식 시 400 반환", "not-a-date-time"))

    schema_type = schema.get("type")
    if schema_type in {"integer", "number"}:
        minimum = schema.get("minimum")
        maximum = schema.get("maximum")
        if isinstance(minimum, (int, float)):
            cases.append((f"OpenAPI range 필드 {field.name} 최솟값 미만 시 400 반환", minimum - 1))
        if isinstance(maximum, (int, float)):
            cases.append((f"OpenAPI range 필드 {field.name} 최댓값 초과 시 400 반환", maximum + 1))
    elif schema_type == "string":
        min_length = schema.get("minLength")
        max_length = schema.get("maxLength")
        pattern = schema.get("pattern")

        if isinstance(min_length, int) and min_length > 0:
            invalid = "" if min_length == 1 else "x" * (min_length - 1)
            cases.append((f"OpenAPI minLength 필드 {field.name} 최소 길이 미만 시 400 반환", invalid))
        if isinstance(max_length, int):
            cases.append((f"OpenAPI maxLength 필드 {field.name} 최대 길이 초과 시 400 반환", "X" * (max_length + 1)))
        if isinstance(pattern, str) and pattern:
            invalid_len = 1
            if isinstance(min_length, int):
                invalid_len = max(invalid_len, min_length)
            if isinstance(max_length, int) and invalid_len > max_length:
                invalid_len = max_length
            if invalid_len > 0:
                cases.append((f"OpenAPI pattern 필드 {field.name} 패턴 위반 시 400 반환", "!" * invalid_len))

    return cases


def matcher_for(schema: dict[str, Any]) -> str:
    schema_type = schema.get("type")
    if schema_type in {"integer", "number"}:
        return "#number"
    if schema_type == "boolean":
        return "#boolean"
    if schema_type == "array":
        return "#array"
    if schema_type == "object":
        return "#object"
    return "#string"


def response_properties(operation: Operation) -> dict[str, Any]:
    properties = operation.response_schema.get("properties", {})
    return properties if isinstance(properties, dict) else {}


def response_field_names(operation: Operation) -> set[str]:
    return set(response_properties(operation))


def response_match_lines(
    schema: dict[str, Any],
    indent: str = "    ",
    expected_values: dict[str, str] | None = None,
) -> list[str]:
    schema = schema or {}
    expected_values = expected_values or {}
    properties = schema.get("properties", {})
    if not isinstance(properties, dict) or not properties:
        return [f"{indent}And match response == '#object'"]

    required = list(schema.get("required", []))
    names = required or list(properties.keys())
    lines = []
    for name in names[:MAX_RESPONSE_ASSERTION_FIELDS]:
        if name in expected_values:
            lines.append(f"{indent}And match response.{name} == {expected_values[name]}")
            continue
        prop = properties.get(name, {})
        lines.append(f"{indent}And match response.{name} == '{matcher_for(prop)}'")
    return lines


def feature_header(
    org: str,
    service: str,
    api: str,
    title: str,
    extra_tags: list[str] | None = None,
) -> list[str]:
    tags = [f"@service={service}", f"@api={api}"]
    if extra_tags:
        tags.extend(f"@{tag}" for tag in extra_tags)
    return [
        " ".join(tags),
        f"Feature: {title}",
        "",
        "  Background:",
        "    * url gatewayUrl",
        f"    * def org = '{org}'",
        f"    * def service = '{service}'",
        "    * def basePath = '/cats/' + org + '/' + service",
        "",
    ]


def is_dynamic_id_field(field_name: str, operations: list[Operation]) -> bool:
    if field_name in STABLE_ID_FIELDS:
        return False
    if field_name in SAMPLE_VALUES and "NONEXISTENT" in str(SAMPLE_VALUES[field_name]):
        return True
    if not field_name.endswith("Id"):
        return False
    return any(field_name in response_field_names(operation) for operation in operations)


def is_create_operation(operation: Operation) -> bool:
    return operation.success_status == 201 or operation.api.startswith(CREATE_API_PREFIXES)


def is_terminal_operation(operation: Operation) -> bool:
    return operation.api.startswith(TERMINAL_API_PREFIXES)


def producer_candidates(field_name: str, consumer: Operation, operations: list[Operation]) -> list[Operation]:
    candidates = [
        operation
        for operation in operations
        if operation.api != consumer.api and field_name in response_field_names(operation)
    ]
    return sorted(candidates, key=lambda operation: (0 if is_create_operation(operation) else 1, operation.api))


def setup_links(operation: Operation, operations: list[Operation]) -> list[Link]:
    links: list[Link] = []
    seen: set[tuple[str, str]] = set()
    for field in operation.request_fields:
        if not field.required or not is_dynamic_id_field(field.name, operations):
            continue
        candidates = producer_candidates(field.name, operation, operations)
        if not candidates:
            continue
        key = (candidates[0].api, field.name)
        if key in seen:
            continue
        seen.add(key)
        links.append(
            Link(
                producer=candidates[0],
                consumer=operation,
                field_name=field.name,
                reason=f"{candidates[0].api} 응답의 {field.name}을 {operation.api} 요청에 사용",
            )
        )
    return links


def cleanup_operation_for(field_name: str, current: Operation, operations: list[Operation]) -> Operation | None:
    if is_terminal_operation(current):
        return None
    candidates = [
        operation
        for operation in operations
        if operation.api != current.api
        and is_terminal_operation(operation)
        and any(field.name == field_name for field in operation.request_fields)
    ]
    return sorted(candidates, key=lambda operation: operation.api)[0] if candidates else None


def render_call(
    api: str,
    operation: Operation,
    overrides: dict[str, Any] | None = None,
    include_response_matches: bool = True,
    expected_response_values: dict[str, str] | None = None,
) -> list[str]:
    lines = [
        f"    Given path basePath + '/{api}'",
        f"    And request {request_object(operation.request_fields, overrides=overrides)}",
        "    When method POST",
        f"    Then status {operation.success_status}",
    ]
    if include_response_matches:
        lines.extend(response_match_lines(operation.response_schema, expected_values=expected_response_values))
    return lines


def expected_expression(value: Any) -> str:
    if isinstance(value, str) and value.startswith("#(") and value.endswith(")"):
        return value[2:-1]
    return karate_value(value)


def expected_response_values_from_request(
    operation: Operation,
    overrides: dict[str, Any] | None = None,
) -> dict[str, str]:
    overrides = overrides or {}
    response_fields = response_field_names(operation)
    expected: dict[str, str] = {}
    for field in operation.request_fields:
        if field.name not in response_fields:
            continue
        value = overrides[field.name] if field.name in overrides else sample_for(field)
        expected[field.name] = expected_expression(value)
    for field_name, expression in expected_values_from_response_schema(operation).items():
        expected.setdefault(field_name, expression)
    return expected


def expected_values_from_response_schema(operation: Operation) -> dict[str, str]:
    expected: dict[str, str] = {}
    for name, schema in response_properties(operation).items():
        enum = schema.get("enum") if isinstance(schema, dict) else None
        if not isinstance(enum, list) or not enum:
            continue
        if len(enum) == 1:
            expected[name] = karate_value(enum[0])
            continue
        allowed = " || ".join(f'"{value}"' for value in enum)
        expected[name] = f"'#({allowed})'"
    return expected


def api_feature(org: str, service: str, operation: Operation, operations: list[Operation]) -> str:
    title = f"{org} {service} {operation.summary or operation.api} 단건 API 테스트"
    lines = feature_header(org, service, operation.api, title, extra_tags=["kind=single-api"])

    links = setup_links(operation, operations)
    request_overrides = {link.field_name: f"#({link.field_name})" for link in links}
    cleanup_field = next((link.field_name for link in links), None)
    if cleanup_field is None:
        cleanup_field = next(
            (field for field in response_field_names(operation) if is_dynamic_id_field(field, operations)),
            None,
        )
    cleanup_operation = cleanup_operation_for(cleanup_field, operation, operations) if cleanup_field else None

    lines.append("  Scenario: 기본 요청 성공")
    for link in links:
        lines.append(f"    # 사전 조건: {link.producer.api} 응답 필드 {link.field_name} 값을 요청에 사용")
        lines.extend(render_call(link.producer.api, link.producer, include_response_matches=False))
        lines.append(f"    And match response.{link.field_name} == '#string'")
        lines.append(f"    * def {link.field_name} = response.{link.field_name}")
        lines.append("")

    lines.extend(
        render_call(
            operation.api,
            operation,
            overrides=request_overrides,
            expected_response_values=expected_response_values_from_request(operation, request_overrides),
        )
    )

    if cleanup_operation and cleanup_field:
        if cleanup_field in response_field_names(operation) and not links:
            lines.append(f"    * def {cleanup_field} = response.{cleanup_field}")
        lines.extend(
            [
                "",
                f"    # 정리: {cleanup_operation.api} 호출로 테스트 데이터 상태를 종료",
            ]
        )
        lines.extend(
            render_call(
                cleanup_operation.api,
                cleanup_operation,
                overrides={cleanup_field: f"#({cleanup_field})"},
                include_response_matches=False,
            )
        )

    for field in [field for field in operation.request_fields if field.required]:
        lines.extend(
            [
                "",
                f"  Scenario: OpenAPI required 필드 {field.name} 누락 시 400 반환",
                f"    Given path basePath + '/{operation.api}'",
                f"    And request {request_object(operation.request_fields, omit=field.name)}",
                "    When method POST",
                "    Then status 400",
            ]
        )
    for field in operation.request_fields:
        for title, invalid_value in invalid_constraint_cases(field):
            lines.extend(
                [
                    "",
                    f"  Scenario: {title}",
                    f"    Given path basePath + '/{operation.api}'",
                    f"    And request {request_object(operation.request_fields, overrides={field.name: invalid_value})}",
                    "    When method POST",
                    "    Then status 400",
                ]
            )
    lines.append("")
    return "\n".join(lines)


def lifecycle_feature(org: str, service: str, operations: dict[str, Operation]) -> str:
    config = LIFECYCLES.get(service)
    if not config:
        raise SystemExit(f"인식된 lifecycle 시나리오가 없습니다: service={service}")
    missing = [api for api in config["steps"] if api not in operations]
    if missing:
        raise SystemExit(f"lifecycle 생성에 필요한 API가 없습니다: {', '.join(missing)}")

    id_field = str(config["id_field"])
    id_var = str(config["id_var"])
    lines = feature_header(
        org,
        service,
        str(config["scenario_api"]),
        str(config["feature"]),
        extra_tags=["kind=scenario"],
    )
    lines.append("  Scenario: 생성 후 조회하고 종료 상태로 변경한다")

    for index, api in enumerate(config["steps"]):
        operation = operations[api]
        overrides: dict[str, Any] = {}
        if index > 0:
            overrides[id_field] = f"#({id_var})"
        expected_id = "'#string'" if index == 0 else id_var
        lines.extend(
            [
                f"    Given path basePath + '/{api}'",
                f"    And request {request_object(operation.request_fields, overrides=overrides)}",
                "    When method POST",
                f"    Then status {operation.success_status}",
                f"    And match response.{id_field} == {expected_id}",
            ]
        )
        expected_status = config["status"].get(api)
        if expected_status:
            lines.append(f"    And match response.status == '{expected_status}'")
        if index == 0:
            lines.append(f"    * def {id_var} = response.{id_field}")
        if index < len(config["steps"]) - 1:
            lines.append("")
    lines.append("")
    return "\n".join(lines)


def operation_summary(org: str, service: str, operations: list[Operation]) -> str:
    lines = [f"org={org} service={service}", ""]
    for operation in operations:
        request_fields = ", ".join(
            f"{field.name}{'*' if field.required else ''}:{field.source}" for field in operation.request_fields
        )
        response_props = operation.response_schema.get("properties", {})
        response_fields = ", ".join(response_props.keys()) if isinstance(response_props, dict) else ""
        lines.extend(
            [
                f"- api: {operation.api}",
                f"  plain: {operation.method} {operation.path}",
                f"  operationId: {operation.operation_id}",
                f"  success: {operation.success_status}",
                f"  request: {request_fields or '(none)'}",
                f"  response: {response_fields or '(object)'}",
            ]
        )
    return "\n".join(lines)


def data_flow_graph(org: str, service: str, operations: list[Operation]) -> str:
    links: list[Link] = []
    for producer in operations:
        output_fields = response_field_names(producer)
        if not output_fields:
            continue
        for consumer in operations:
            if producer.api == consumer.api:
                continue
            for field in consumer.request_fields:
                if field.name not in output_fields:
                    continue
                confidence = "high" if field.required or field.name.endswith("Id") else "medium"
                links.append(
                    Link(
                        producer=producer,
                        consumer=consumer,
                        field_name=field.name,
                        reason=f"{confidence}: response.{field.name} -> request.{field.name}",
                    )
                )

    lines = [f"org={org} service={service}", ""]
    if not links:
        lines.append("연결 후보가 없습니다. OpenAPI 요약, 설명, 예시를 읽고 Swagger 문서 안에서만 판단하세요.")
        return "\n".join(lines)

    for link in sorted(links, key=lambda item: (item.consumer.api, item.producer.api, item.field_name)):
        lines.append(f"- {link.producer.api} -> {link.consumer.api} via {link.field_name} ({link.reason})")
    return "\n".join(lines)


def output_path(output_root: Path, service: str, case_name: str) -> Path:
    case_dir = output_root / service / kebab(case_name)
    return case_dir / f"{kebab(case_name)}.feature"


def write_feature(path: Path, content: str, overwrite: bool, skip_existing: bool) -> None:
    if path.exists() and skip_existing:
        print(f"SKIP {path}")
        return
    if path.exists() and not overwrite:
        raise SystemExit(f"이미 파일이 있습니다. --overwrite로 명시하세요: {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    print(path)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="OpenAPI에서 Karate feature 초안을 생성합니다.")
    parser.add_argument("--openapi", required=True, type=Path, help="OpenAPI YAML 경로")
    parser.add_argument("--mode", choices=["summary", "graph", "api", "scenario"], required=True)
    parser.add_argument("--api", help="단건 API 이름. 생략하면 모든 API를 생성합니다.")
    parser.add_argument("--output-root", type=Path, default=Path("karate-tests/src/test/resources/scenarios"))
    parser.add_argument("--dry-run", action="store_true", help="파일을 쓰지 않고 stdout에 출력합니다.")
    parser.add_argument("--overwrite", action="store_true", help="기존 feature 파일을 덮어씁니다.")
    parser.add_argument("--skip-existing", action="store_true", help="이미 있는 feature 파일은 건너뜁니다.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    spec = load_spec(args.openapi)
    org, service, operations = collect_operations(spec)
    operations_by_api = {operation.api: operation for operation in operations}

    if args.mode == "summary":
        print(operation_summary(org, service, operations))
        return 0
    if args.mode == "graph":
        print(data_flow_graph(org, service, operations))
        return 0

    generated: list[tuple[Path, str]] = []
    if args.mode == "api":
        if args.api and args.api not in operations_by_api:
            available = ", ".join(sorted(operations_by_api))
            raise SystemExit(f"API를 찾지 못했습니다: {args.api}. 사용 가능: {available}")
        selected = [operations_by_api[args.api]] if args.api else operations
        for operation in selected:
            generated.append(
                (output_path(args.output_root, service, operation.api), api_feature(org, service, operation, operations))
            )
    else:
        config = LIFECYCLES.get(service)
        if not config:
            raise SystemExit(f"지원하지 않는 scenario service입니다: {service}")
        generated.append(
            (
                output_path(args.output_root, service, str(config["name"])),
                lifecycle_feature(org, service, operations_by_api),
            )
        )

    if args.dry_run:
        for path, content in generated:
            print(f"# path: {path}")
            print(content)
        return 0

    for path, content in generated:
        write_feature(path, content, args.overwrite, args.skip_existing)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
