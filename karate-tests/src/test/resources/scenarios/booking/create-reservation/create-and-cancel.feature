@service=booking @api=createReservation
Feature: 예약 생성 → 상세 조회 → 취소 흐름

  Background:
    * url gatewayUrl

  Scenario: 예약 전체 생명주기
    # 1. 예약 생성
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001', scheduleId: 'R-001-2026-02-26-9', userId: 'user-karate-001', quantity: 1, memo: 'karate test' }
    When method POST
    Then status 201
    And match response.reservationId == '#string'
    And match response.status == '#("CREATED" || "CONFIRMED" || "WAITLISTED")'
    And match response.createdAt == '#string'
    * def reservationId = response.reservationId

    # 2. 예약 상세 조회
    Given path basePath + '/getReservationDetail'
    And request { reservationId: '#(reservationId)' }
    When method POST
    Then status 200
    And match response.reservationId == reservationId
    And match response.userId == 'user-karate-001'
    And match response.quantity == 1
    And match response.status == '#string'

    # 3. 예약 취소
    Given path basePath + '/cancelReservation'
    And request { reservationId: '#(reservationId)', reason: 'karate test teardown' }
    When method POST
    Then status 200
    And match response.reservationId == reservationId
    And match response.status == 'CANCELED'
    And match response.canceledAt == '#string'

  Scenario: 필수 필드 누락 시 400 반환
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001' }
    When method POST
    Then status 400
