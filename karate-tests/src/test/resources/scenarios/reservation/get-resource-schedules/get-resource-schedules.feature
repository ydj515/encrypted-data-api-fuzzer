@service=reservation @api=getResourceSchedules
Feature: orgA reservation 자원 스케줄 조회(본문 기반) 단건 API 테스트

  Background:
    * url gatewayUrl
    * def org = 'orgA'
    * def service = 'reservation'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    Given path basePath + '/getResourceSchedules'
    And request { resourceId: 'R-001', from: '2026-02-26T09:00:00+09:00', to: '2026-02-26T18:00:00+09:00' }
    When method POST
    Then status 200
    And match response.resourceId == 'R-001'
    And match response.items == '#array'

  Scenario: OpenAPI required 필드 resourceId 누락 시 400 반환
    Given path basePath + '/getResourceSchedules'
    And request { from: '2026-02-26T09:00:00+09:00', to: '2026-02-26T18:00:00+09:00' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 from 누락 시 400 반환
    Given path basePath + '/getResourceSchedules'
    And request { resourceId: 'R-001', to: '2026-02-26T18:00:00+09:00' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 to 누락 시 400 반환
    Given path basePath + '/getResourceSchedules'
    And request { resourceId: 'R-001', from: '2026-02-26T09:00:00+09:00' }
    When method POST
    Then status 400

  Scenario: OpenAPI format 필드 from 잘못된 date-time 형식 시 400 반환
    Given path basePath + '/getResourceSchedules'
    And request { resourceId: 'R-001', from: 'not-a-date-time', to: '2026-02-26T18:00:00+09:00' }
    When method POST
    Then status 400

  Scenario: OpenAPI format 필드 to 잘못된 date-time 형식 시 400 반환
    Given path basePath + '/getResourceSchedules'
    And request { resourceId: 'R-001', from: '2026-02-26T09:00:00+09:00', to: 'not-a-date-time' }
    When method POST
    Then status 400
