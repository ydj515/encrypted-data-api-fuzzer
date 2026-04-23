@service=booking @api=getResourceSchedules
Feature: 자원 스케줄 조회

  Background:
    * url gatewayUrl

  Scenario: 시간 범위 내 스케줄 조회 성공
    Given path basePath + '/getResourceSchedules'
    And request { resourceId: 'R-001', from: '2026-04-23T09:00:00+09:00', to: '2026-04-23T18:00:00+09:00' }
    When method POST
    Then status 200
    And match response.resourceId == 'R-001'
    And match response.items == '#array'
    And match response.items == '#[_ > 0]'
    And match each response.items == { scheduleId: '#string', startAt: '#string', endAt: '#string', status: '#string' }

  Scenario: 결과 스케줄이 시간 순으로 정렬됨
    Given path basePath + '/getResourceSchedules'
    And request { resourceId: 'R-001', from: '2026-04-23T09:00:00+09:00', to: '2026-04-23T12:00:00+09:00' }
    When method POST
    Then status 200
    And match response.items[0].scheduleId == 'R-001-2026-04-23-9'
    And match response.items[1].scheduleId == 'R-001-2026-04-23-10'
    And match response.items[2].scheduleId == 'R-001-2026-04-23-11'

  Scenario: from이 to보다 나중이면 400 반환
    Given path basePath + '/getResourceSchedules'
    And request { resourceId: 'R-001', from: '2026-04-23T18:00:00+09:00', to: '2026-04-23T09:00:00+09:00' }
    When method POST
    Then status 400

  Scenario: 존재하지 않는 자원 스케줄 조회 시 404 반환
    Given path basePath + '/getResourceSchedules'
    And request { resourceId: 'R-999', from: '2026-04-23T09:00:00+09:00', to: '2026-04-23T18:00:00+09:00' }
    When method POST
    Then status 404
