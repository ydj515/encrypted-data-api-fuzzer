@service=booking @api=listDailySchedules
Feature: 일별 전체 스케줄 조회

  Background:
    * url gatewayUrl

  Scenario: 일별 스케줄 조회 성공 — 모든 자원 포함
    Given path basePath + '/listDailySchedules'
    And request { date: '2026-04-23' }
    When method POST
    Then status 200
    And match response.date == '#string'
    And match response.items == '#array'
    And match response.items == '#[_ > 0]'
    And match each response.items == { resourceId: '#string', scheduleId: '#string', startAt: '#string', endAt: '#string', status: '#string' }

  Scenario: 12개 자원에 대한 스케줄이 모두 반환됨
    Given path basePath + '/listDailySchedules'
    And request { date: '2026-04-23' }
    When method POST
    Then status 200
    And match response.items == '#[12]'

  Scenario: 스케줄 ID 형식 검증 — {resourceId}-{date}-9
    Given path basePath + '/listDailySchedules'
    And request { date: '2026-04-23' }
    When method POST
    Then status 200
    And match response.items[0].scheduleId == 'R-001-2026-04-23-9'
    And match response.items[0].status == 'AVAILABLE'
