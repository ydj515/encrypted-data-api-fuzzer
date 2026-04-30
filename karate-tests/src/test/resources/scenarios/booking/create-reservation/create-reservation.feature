@service=booking @api=createReservation @kind=single-api
Feature: catsOrg booking 예약 생성 단건 API 테스트

  Background:
    * url gatewayUrl
    * def org = 'catsOrg'
    * def service = 'booking'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001', scheduleId: 'R-001-2026-02-26-9', userId: 'user-karate-001', quantity: 1, memo: 'karate generated test' }
    When method POST
    Then status 201
    And match response.reservationId == '#string'
    And match response.status == 'CREATED'
    And match response.createdAt == '#string'
    * def reservationId = response.reservationId

    # 정리: cancelReservation 호출로 테스트 데이터 상태를 종료
    Given path basePath + '/cancelReservation'
    And request { reservationId: '#(reservationId)', reason: 'karate generated teardown' }
    When method POST
    Then status 200

  Scenario: OpenAPI required 필드 resourceId 누락 시 400 반환
    Given path basePath + '/createReservation'
    And request { scheduleId: 'R-001-2026-02-26-9', userId: 'user-karate-001', quantity: 1, memo: 'karate generated test' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 scheduleId 누락 시 400 반환
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001', userId: 'user-karate-001', quantity: 1, memo: 'karate generated test' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 userId 누락 시 400 반환
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001', scheduleId: 'R-001-2026-02-26-9', quantity: 1, memo: 'karate generated test' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 quantity 누락 시 400 반환
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001', scheduleId: 'R-001-2026-02-26-9', userId: 'user-karate-001', memo: 'karate generated test' }
    When method POST
    Then status 400

  Scenario: OpenAPI range 필드 quantity 최솟값 미만 시 400 반환
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001', scheduleId: 'R-001-2026-02-26-9', userId: 'user-karate-001', quantity: 0, memo: 'karate generated test' }
    When method POST
    Then status 400

  Scenario: OpenAPI range 필드 quantity 최댓값 초과 시 400 반환
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001', scheduleId: 'R-001-2026-02-26-9', userId: 'user-karate-001', quantity: 10000, memo: 'karate generated test' }
    When method POST
    Then status 400
