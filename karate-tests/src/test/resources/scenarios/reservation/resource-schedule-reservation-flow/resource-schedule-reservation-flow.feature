@service=reservation @api=createReservation @scenario=llm-data-flow
Feature: orgA reservation 스케줄 응답 기반 예약 흐름

  Background:
    * url gatewayUrl
    * def org = 'orgA'
    * def service = 'reservation'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 일별 스케줄 응답의 resourceId와 scheduleId로 예약을 생성하고 취소한다
    Given path basePath + '/listDailySchedules'
    And request { date: '2026-02-26' }
    When method POST
    Then status 200
    And match response.items == '#[_ > 0]'
    * def scheduleDate = response.date
    * def resourceId = response.items[0].resourceId
    * def scheduleId = response.items[0].scheduleId

    Given path basePath + '/getResourceInventory'
    And request { resourceId: '#(resourceId)', date: '#(scheduleDate)' }
    When method POST
    Then status 200
    And match response.resourceId == resourceId
    And match response.date == scheduleDate
    And assert response.availableQuantity > 0

    Given path basePath + '/createReservation'
    And request { resourceId: '#(resourceId)', scheduleId: '#(scheduleId)', userId: 'llm-flow-user-001', quantity: 1, memo: 'llm data-flow scenario' }
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
    And match response.resourceId == resourceId
    And match response.scheduleId == scheduleId
    And match response.userId == 'llm-flow-user-001'

    Given path basePath + '/cancelReservation'
    And request { reservationId: '#(reservationId)', reason: 'llm data-flow teardown' }
    When method POST
    Then status 200
    And match response.reservationId == reservationId
    And match response.status == 'CANCELED'
