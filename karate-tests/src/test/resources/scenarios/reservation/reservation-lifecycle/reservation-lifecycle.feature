@service=reservation @api=reservationLifecycle @kind=scenario
Feature: orgA 예약 서비스 예약 전체 생명주기

  Background:
    * url gatewayUrl
    * def org = 'orgA'
    * def service = 'reservation'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 생성 후 조회하고 종료 상태로 변경한다
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001', scheduleId: 'R-001-2026-02-26-9', userId: 'user-karate-001', quantity: 1, memo: 'karate generated test' }
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
    And match response.status == 'CREATED'

    Given path basePath + '/cancelReservation'
    And request { reservationId: '#(reservationId)', reason: 'karate generated teardown' }
    When method POST
    Then status 200
    And match response.reservationId == reservationId
    And match response.status == 'CANCELED'
