@service=reservation @api=listDailySchedules
Feature: orgA reservation 일별 스케줄 목록 조회(본문 기반) 단건 API 테스트

  Background:
    * url gatewayUrl
    * def org = 'orgA'
    * def service = 'reservation'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    Given path basePath + '/listDailySchedules'
    And request { date: '2026-02-26' }
    When method POST
    Then status 200
    And match response.date == '2026-02-26'
    And match response.items == '#array'

  Scenario: OpenAPI required 필드 date 누락 시 400 반환
    Given path basePath + '/listDailySchedules'
    And request { }
    When method POST
    Then status 400

  Scenario: OpenAPI format 필드 date 잘못된 date 형식 시 400 반환
    Given path basePath + '/listDailySchedules'
    And request { date: 'not-a-date' }
    When method POST
    Then status 400
