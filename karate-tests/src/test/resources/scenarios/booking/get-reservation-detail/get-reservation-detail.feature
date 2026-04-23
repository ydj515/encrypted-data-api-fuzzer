@service=booking @api=getReservationDetail
Feature: 예약 상세정보 조회

  Background:
    * url gatewayUrl

  Scenario: 예약 생성 후 상세 조회 성공
    # 사전 조건: 예약 생성하여 실제 reservationId 확보
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001', scheduleId: 'R-001-2026-02-26-9', userId: 'user-001', quantity: 1, memo: 'cats test' }
    When method POST
    Then status 201
    * def reservationId = response.reservationId

    # 상세 조회
    Given path basePath + '/getReservationDetail'
    And request { reservationId: '#(reservationId)' }
    When method POST
    Then status 200
    And match response.reservationId == '#string'
    And match response.resourceId == '#string'
    And match response.scheduleId == '#string'
    And match response.userId == '#string'
    And match response.quantity == '#number'
    And match response.status == '#("CREATED" || "CONFIRMED" || "WAITLISTED" || "CANCELED")'
    And match response.createdAt == '#string'

  Scenario: 존재하지 않는 예약 ID 조회 시 404 반환
    Given path basePath + '/getReservationDetail'
    And request { reservationId: 'RSV-NONEXISTENT-999' }
    When method POST
    Then status 404

  Scenario: 필수 필드 누락 시 400 반환
    Given path basePath + '/getReservationDetail'
    And request { }
    When method POST
    Then status 400
