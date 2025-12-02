# 코드 스타일 & 리뷰 가이드 (encrypted-data-api-fuzzer)

## 원칙
- **가독성/일관성**: 팀원이 바로 읽고 유지보수할 수 있게 간단·일관된 패턴 유지.
- **안전성**: 암복호화/라우팅 경로가 깨지지 않도록 방어적 코딩, 명확한 오류 처리.
- **테스트 가능성**: 단위/슬라이스 테스트로 핵심 흐름을 검증 가능하게 작성.
- **관측성**: 로그는 재현/트러블슈팅에 필요한 최소 정보만 레벨에 맞게 남긴다.

## Java/Spring 스타일
- **라인 길이**: 120자 이하 권장.
- **들여쓰기**: 스페이스 4칸.
- **패키지 구조**: `config/crypto/controller/service/exception` 등 역할 기준으로 분리.
- **네이밍**: 클래스 PascalCase, 메서드·변수 camelCase. 상수는 UPPER_SNAKE_CASE.
- **Optional 사용**: 컨트롤러 반환 외에는 `Optional` 남기지 않고 즉시 처리.
- **Lombok**: 필수 아님. 생성자 주입을 기본으로 사용.
- **Null 처리**: 들어오는 값은 최대한 validation/방어적 체크. 외부 응답 파싱 시 기본값을 안전하게 처리.

## Gateway 코드 관례
- **DTO 지양**: `/cats/{org}/{service}/{api}`는 `@RequestBody String`으로 받고 내용은 그대로 암호화.
- **라우트 탐색**: `(org, service, api, method)` 기준으로 `GatewayProperties` 조회. 미존재 시 404.
- **암복호화**: `CryptoModule`/`ChecksumModule` 인터페이스 사용. 로컬 기본은 Base64 + SHA-256 스텁.
- **외부 호출**: 헤더(`X-Checksum`, `X-Api-Key`, `X-Ins-Code` 등)와 바디(`{"data": ...}`)를 명확히 세팅.
- **복호화 실패 처리**: `data` 없거나 복호화 오류 시 경고 로그 후 원본 응답 반환해 디버깅 가능하게.

## 로깅
- **레벨**: DEBUG(개발용 상세), INFO(주요 경로), WARN(정상 흐름 외지만 계속 처리 가능), ERROR(즉시 주의).
- **민감정보**: 키/암호문/평문 데이터는 로그에 남기지 않는다. 필요 시 prefix 정도만 표시.
- **컨텍스트**: org/service/api, target URL 정도는 남겨 재현 가능하게 한다.

## 예외 처리
- **도메인 예외**: 라우트 미존재 → `RouteNotFoundException` → 404.
- **일반 예외**: 500 + 짧은 메시지. 스택은 서버 로그에만.
- **메시지**: 사용자가 이해할 수 있는 짧은 한국어/영어 혼용 문구 허용, 내부 구현 상세는 숨김.

## 테스트
- **우선순위**: 라우팅 조회, 암복호화 모듈 호출 여부, 외부 호출 시 헤더/바디 구성, 응답 복호화.
- **도구**: JUnit5 + Spring `@WebMvcTest`/`RestClient` 목킹. WireMock 등으로 외부 API 모킹 가능.
- **커맨드 기록**: 실행한 테스트 커맨드를 리뷰 코멘트에 남긴다 (`./gradlew test` 등).

## 설정/스크립트
- **application.yml**: `gateway.apis`에 기관/서비스/API별 설정을 추가. 비밀 값은 로컬 오버라이드 파일 활용.
- **쉘 스크립트**: `set -euo pipefail` 기본, 상대 경로는 스크립트 기준 절대경로로 변환.

## PR/리뷰 체크리스트
- 요구사항/Task.md와 동작 일치 여부 확인.
- 공통 헤더/암복호화/라우팅이 깨지지 않았는지 확인.
- 로그/예외 메시지가 과도하거나 민감정보 노출하지 않는지 점검.
- 필요한 테스트 추가/갱신 여부 확인.

## 예시 스니펫 (Gateway 흐름)
```java
@PostMapping("/{org}/{service}/{api}")
public ResponseEntity<String> proxy(@PathVariable String org,
                                    @PathVariable String service,
                                    @PathVariable String api,
                                    @RequestBody String body) {
    ApiRoute route = gatewayProperties.find(org, service, api, "POST")
            .orElseThrow(() -> new RouteNotFoundException("route not found"));
    String encrypted = cryptoModule.encrypt(route.key(), body);
    String checksum = checksumModule.checksum(encrypted);

    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Checksum", checksum);
    // ... X-Api-Key, X-Ins-Code 등 추가

    ResponseEntity<String> upstream = restClient.post()
            .uri(route.host() + route.externalPath())
            .headers(h -> h.putAll(headers))
            .body(Map.of("data", encrypted))
            .retrieve()
            .toEntity(String.class);

    String decrypted = cryptoModule.decrypt(route.key(), JsonPath.read(upstream.getBody(), "$.data"));
    return ResponseEntity.status(upstream.getStatusCode()).body(decrypted);
}
```
