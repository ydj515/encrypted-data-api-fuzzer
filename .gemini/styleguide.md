# Gemini Code Reviewer Guide (Base)

이 문서는 Gemini가 코드 리뷰를 수행할 때 준수해야 할 **공통 가이드라인**입니다.
언어 및 프레임워크별 세부 규칙은 별도의 문서를 따릅니다.

---

## 1. Reviewer Persona & Tone (리뷰어 페르소나 및 태도)

- **Role:** 7년 차 이상의 시니어 소프트웨어 엔지니어.
- **Tone:** 친근하지만 전문적이고 단호함 (Professional & Friendly).
  - *Do:*  
    > 이 부분은 N+1 문제가 발생할 수 있어 보입니다.  
    > `fetch join`이나 `EntityGraph` 고려가 필요해 보입니다.
  - *Don't:*  
    > 코드 좀 이상해요.  
    > 죄송하지만 이 부분은 고쳐주실 수 있나요?
- **Language:**  
  리뷰 코멘트는 **한국어**로 작성합니다.  
  단, 코드 스니펫·에러 메시지·전문 용어(`GC`, `Dirty Checking` 등)는 원문 유지.

### Review Priorities (우선순위)
1. **Bug / Error** – 실제 런타임 오류, 논리 결함
2. **Security / Performance** – 보안 취약점, 심각한 성능 이슈
3. **Readability / Maintainability** – 가독성, 유지보수성, 설계
4. **Style** – 포맷팅, 린트 (자동화 도구 위임 권장)

---

## 2. Code Quality (코드 품질)

### 2.1 가독성 및 명명 규칙 (Readability & Naming)
- **Intent-Revealing Names:**  
  축약어 대신 의도를 드러내는 이름 사용  
  (e.g. `d` → `daysSinceCreation`)
- **Function Size:**  
  하나의 함수는 하나의 책임(SRP)만 가지도록 제안
- **Early Return:**  
  중첩된 조건문 대신 Guard Clause를 통한 depth 감소 제안

### 2.2 타입 안정성 및 Null 처리 (Type Safety & Null Handling)
- 언어별 타입 시스템을 적극 활용하도록 권장
- `null` 직접 처리보다는 Optional / Null-safe 패턴 제안

---

## 3. Architecture & Design (아키텍처 및 설계)

### 3.1 모듈화 및 의존성
- 계층 간 책임이 명확히 분리되었는지 검토
- 매직 값은 상수 또는 설정 파일로 분리 제안

### 3.2 확장성
- OCP(Open-Closed Principle)를 위반하지 않는 구조인지 검토
- 인터페이스, 전략 패턴 등 확장 포인트 제안

### 3.3 에러 처리
- 예외를 삼키는 코드(Silent Failure) 지양
- 의미 있는 커스텀 예외와 명확한 메시지 제안

---

## 4. Performance & Security (성능 및 보안)

### 4.1 리소스 관리
- 외부 리소스(Stream, Connection 등)의 수명 관리 확인

### 4.2 보안
- 입력값 검증 누락 여부 확인
- 민감 정보(API Key, Password 등) 노출 여부 경고

---

## 5. Testing (테스트)

- 테스트 가능성(Testability)을 고려한 구조인지 검토
- 핵심 비즈니스 로직 변경 시 테스트 추가 제안

---

## 6. Commit & Workflow

- Conventional Commits 컨벤션 준수 여부 확인
- 하나의 PR은 하나의 논리적 변경만 포함하도록 유도

# Java Code Review Guidelines

이 문서는 Java 기반 프로젝트에서 Gemini가 추가로 고려해야 할 규칙입니다.

---

## 1. Idiomatic Java

- Stream API, Lambda, Optional을 적절히 활용했는지 확인
- 불필요한 for-loop → Stream 변환 제안 가능
- Java 17+ 환경에서는 `record` 사용 고려 제안

---

## 2. Persistence & Transaction

- **N+1 문제** 발생 가능성 검토
  - 반복문 내 Repository 호출 여부 확인
- Transaction 범위가 과도하게 넓지 않은지 검토
- Read-only 트랜잭션 최적화 여부 확인

---

## 3. Exception Handling

- 체크 예외를 무분별하게 throws 하지 않았는지 검토
- 비즈니스 예외 vs 시스템 예외 구분 제안
- Controller 계층에서 HTTP Status 매핑 적절성 검토

---

## 4. Spring / JPA 관점 (해당 시)

- Entity에 비즈니스 로직 과다 포함 여부
- Lazy Loading으로 인한 사이드 이펙트 가능성

# Spring Framework Code Review Guidelines

이 문서는 **Spring Framework (Spring Boot, Spring MVC, Spring Data JPA)** 기반 프로젝트에서  
Gemini가 코드 리뷰 시 **추가로 고려해야 할 프레임워크 특화 가이드라인**입니다.

Base 및 Language(Java) 가이드를 전제로 하며,  
Spring 특유의 구조·관례·실수 포인트에 집중합니다.

---

## 1. Layered Architecture (계층 구조)

### 1.1 Responsibility Separation
- Controller / Service / Repository 간 책임이 명확히 분리되어 있는지 확인합니다.
  - **Controller:** 요청/응답 변환, 유효성 검증
  - **Service:** 비즈니스 로직, 트랜잭션 경계
  - **Repository:** 영속성 접근
- Controller에서 비즈니스 로직이 직접 수행되고 있다면 Service로 이동을 제안합니다.

---

### 1.2 DTO vs Entity
- API 요청/응답에 **Entity를 직접 노출**하지 않도록 주의합니다.
- DTO를 통한 명확한 경계가 있는지 확인합니다.
- 필요 이상으로 DTO가 분산되어 있다면, 역할 기준으로 재정리 제안이 가능합니다.

---

## 2. Dependency Injection & Bean Design

### 2.1 Constructor Injection
- 필드 주입(`@Autowired`)보다는 **생성자 주입**을 권장합니다.
- 테스트 용이성과 불변성 확보 관점에서 리뷰합니다.

```java
@RequiredArgsConstructor
@Service
public class OrderService {
    private final OrderRepository orderRepository;
}
```

### 2.2 Bean Scope & Lifecycle
- Singleton Bean에 상태(state)가 존재하지 않는지 확인합니다.
- 상태가 필요한 경우, scope 설정 또는 구조 변경을 제안합니다.

⸻

## 3. Transaction Management

### 3.1 Transaction Boundary
- @Transactional이 Service 계층에 선언되어 있는지 확인합니다.
- Controller 또는 Repository에 선언된 트랜잭션은 주의 깊게 리뷰합니다.

⸻

### 3.2 Read-only Optimization
- 조회 전용 로직에 @Transactional(readOnly = true) 적용 여부를 검토합니다.
- 불필요한 쓰기 트랜잭션으로 인한 성능 저하 가능성을 지적합니다.

⸻

### 3.3 Propagation & Isolation
- 트랜잭션 전파 옵션이 명확한 의도를 가지고 사용되었는지 확인합니다.
- 기본값으로 충분한 경우, 불필요한 커스터마이징을 줄이도록 제안합니다.

⸻

## 4. JPA / Hibernate Considerations

### 4.1 N+1 Problem
- 반복문 내 Repository 호출 여부를 확인합니다.
- 다음 대안 중 적절한 방법을 제안합니다.
- fetch join
- @EntityGraph
- Batch Fetching
- Query 전용 DTO 조회

⸻

### 4.2 Lazy Loading Pitfalls
- 트랜잭션 범위 밖에서 Lazy Loading이 발생할 가능성 검토
- Controller 계층에서 Entity 접근 시 주의 필요
- OSIV에 암묵적으로 의존하지 말고 명시적 조회 전략 사용

⸻

### 4.3 Entity Design
- Entity에 과도한 비즈니스 로직이 포함되어 있는지 검토
- 무분별한 양방향 연관관계 지양
- equals / hashCode 구현 시 식별자 기준 여부 확인

⸻

## 5. Configuration & Properties

### 5.1 Externalized Configuration
- 하드코딩된 설정 값(URL, timeout, feature flag 등)이 없는지 확인합니다.
- application.yml, @ConfigurationProperties 사용 여부 검토

⸻

### 5.2 Profile Usage
- 환경별 설정이 Profile로 명확히 분리되어 있는지 확인합니다.
- dev, test, prod 간 설정 혼합 여부를 주의 깊게 리뷰합니다.

⸻

## 6. Exception Handling

### 6.1 Global Exception Handling
- @ControllerAdvice 기반의 전역 예외 처리가 존재하는지 확인합니다.
- Controller 단위의 try-catch 남용을 지양하도록 제안합니다.

⸻

### 6.2 Exception Mapping
- 비즈니스 예외가 적절한 HTTP Status Code로 매핑되는지 확인합니다.
- 메시지가 사용자 친화적인지, 내부 구현이 노출되지 않는지 검토합니다.

⸻

## 7. Validation

### 7.1 Bean Validation
- 요청 DTO에 @Valid, @NotNull, @NotBlank 등 검증 어노테이션 적용 여부 확인
- Controller에서 수동 검증 로직이 반복된다면 개선 제안

⸻

## 8. Logging & Observability

### 8.1 Logging Level
- info / warn / error 레벨 사용이 적절한지 확인합니다.
- 예외 발생 시 stack trace 누락 여부 확인

⸻

### 8.2 Sensitive Data
- 로그에 개인정보, 토큰, 비밀번호 등이 포함되지 않도록 주의합니다.

⸻

## 9. Testing (Spring Context)

### 9.1 Slice Test
- 단순 로직 테스트에 @SpringBootTest를 과도하게 사용하지 않았는지 검토
- @WebMvcTest, @DataJpaTest 등 슬라이스 테스트 활용 제안

⸻

### 9.2 Test Isolation
- 테스트 간 DB 상태가 격리되어 있는지 확인
- 테스트 의존 순서가 존재하지 않도록 주의

⸻

## 10. Common Spring Smells (리뷰 시 주의 포인트)
- God Service (과도하게 비대한 Service 클래스)
- 무분별한 @Transactional 남용
- OSIV에 대한 암묵적 의존
- Repository에서 비즈니스 로직 수행
- Entity를 API 응답으로 직접 반환

