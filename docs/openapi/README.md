# OpenAPI Layout

- `gateway/`
  - CATS와 Gateway E2E 계약 파일
  - `catalog.yaml`이 Gateway 계약 카탈로그이며, report-server 서비스 목록과 publish 메타의 기준이 됩니다.
  - 예: `cats-gw-openapi.yaml`, `orgA-reservation-gw-openapi.yaml`
- `mock-rest-api-server/`
  - mock REST API 서버 직접 호출용 계약 파일
  - 예: `orgA-reservation-plain-api.yaml`, `orgB-visit-plain-api.yaml`
