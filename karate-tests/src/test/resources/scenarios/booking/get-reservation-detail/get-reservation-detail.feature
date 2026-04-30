@service=booking @api=getReservationDetail @kind=single-api
Feature: catsOrg booking 예약 상세 조회 단건 API 테스트

  Background:
    * url gatewayUrl
    * def org = 'catsOrg'
    * def service = 'booking'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    # 사전 조건: createReservation 응답 필드 reservationId 값을 요청에 사용
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001', scheduleId: 'R-001-2026-02-26-9', userId: 'user-karate-001', quantity: 1, memo: 'karate generated test' }
    When method POST
    Then status 201
    And match response.reservationId == '#string'
    * def reservationId = response.reservationId

    Given path basePath + '/getReservationDetail'
    And request { reservationId: '#(reservationId)' }
    When method POST
    Then status 200
    And match response.reservationId == reservationId
    And match response.resourceId == '#string'
    And match response.scheduleId == '#string'
    And match response.userId == '#string'
    And match response.quantity == '#number'
    And match response.status == '#("CREATED" || "CANCELED")'
    And match response.createdAt == '#string'

    # 정리: cancelReservation 호출로 테스트 데이터 상태를 종료
    Given path basePath + '/cancelReservation'
    And request { reservationId: '#(reservationId)', reason: 'karate generated teardown' }
    When method POST
    Then status 200

  Scenario: OpenAPI required 필드 reservationId 누락 시 400 반환
    Given path basePath + '/getReservationDetail'
    And request { }
    When method POST
    Then status 400

  Scenario: OpenAPI minLength 필드 reservationId 빈 문자열 시 400 반환
    Given path basePath + '/getReservationDetail'
    And request { reservationId: '' }
    When method POST
    Then status 400

  Scenario: OpenAPI maxLength 필드 reservationId 허용 길이 초과 시 400 반환
    Given path basePath + '/getReservationDetail'
    And request { reservationId: 'RSV-1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890X' }
    When method POST
    Then status 400
