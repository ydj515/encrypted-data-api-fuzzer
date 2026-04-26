@service=visit @api=getSiteSlots
Feature: orgB visit site 일별 slot 조회(본문 기반) 단건 API 테스트

  Background:
    * url gatewayUrl
    * def org = 'orgB'
    * def service = 'visit'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    Given path basePath + '/getSiteSlots'
    And request { siteId: 'SITE-01', date: '2026-02-26' }
    When method POST
    Then status 200
    And match response.siteId == 'SITE-01'
    And match response.date == '2026-02-26'
    And match response.totalSlots == '#number'
    And match response.availableSlots == '#number'
    And match response.reservedSlots == '#number'

  Scenario: OpenAPI required 필드 siteId 누락 시 400 반환
    Given path basePath + '/getSiteSlots'
    And request { date: '2026-02-26' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 date 누락 시 400 반환
    Given path basePath + '/getSiteSlots'
    And request { siteId: 'SITE-01' }
    When method POST
    Then status 400

  Scenario: OpenAPI format 필드 date 잘못된 date 형식 시 400 반환
    Given path basePath + '/getSiteSlots'
    And request { siteId: 'SITE-01', date: 'not-a-date' }
    When method POST
    Then status 400
