@service=A @api=createReservation
Feature: orgA A 예약 전체 생명주기

  Background:
    * url gatewayUrl
    * def org = 'orgA'
    * def service = 'A'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 예약 생성 후 조회하고 취소한다
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001', scheduleId: 'R-001-2026-02-26-9', userId: 'orga-user-001', quantity: 1, memo: 'karate orgA create' }
    When method POST
    Then status 201
    And match response.reservationId == '#string'
    And match response.status == 'CREATED'
    * def reservationId = response.reservationId

    Given path basePath + '/getReservationDetail'
    And request { reservationId: '#(reservationId)' }
    When method POST
    Then status 200
    And match response.reservationId == reservationId
    And match response.userId == 'orga-user-001'
    And match response.status == 'CREATED'

    Given path basePath + '/cancelReservation'
    And request { reservationId: '#(reservationId)', reason: 'karate orgA teardown' }
    When method POST
    Then status 200
    And match response.reservationId == reservationId
    And match response.status == 'CANCELED'
    And match response.canceledAt == '#string'
