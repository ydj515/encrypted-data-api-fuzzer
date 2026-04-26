@service=reservation @api=cancelReservation
Feature: orgA reservation 예약 취소 단건 API 테스트

  Background:
    * url gatewayUrl
    * def org = 'orgA'
    * def service = 'reservation'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    # 사전 조건: createReservation 응답 필드 reservationId 값을 요청에 사용
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001', scheduleId: 'R-001-2026-02-26-9', userId: 'user-karate-001', quantity: 1, memo: 'karate generated test' }
    When method POST
    Then status 201
    And match response.reservationId == '#string'
    * def reservationId = response.reservationId

    Given path basePath + '/cancelReservation'
    And request { reservationId: '#(reservationId)', reason: 'karate generated teardown' }
    When method POST
    Then status 200
    And match response.reservationId == reservationId
    And match response.status == 'CANCELED'
    And match response.canceledAt == '#string'

  Scenario: OpenAPI required 필드 reservationId 누락 시 400 반환
    Given path basePath + '/cancelReservation'
    And request { reason: 'karate generated teardown' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 reason 누락 시 400 반환
    Given path basePath + '/cancelReservation'
    And request { reservationId: 'RSV-550e8400-e29b-41d4-a716-446655440000' }
    When method POST
    Then status 400
