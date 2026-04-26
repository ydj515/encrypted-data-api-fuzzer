@service=reservation @api=getResourceInventory
Feature: orgA reservation 자원 재고 확인(본문 기반) 단건 API 테스트

  Background:
    * url gatewayUrl
    * def org = 'orgA'
    * def service = 'reservation'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    Given path basePath + '/getResourceInventory'
    And request { resourceId: 'R-001', date: '2026-02-26' }
    When method POST
    Then status 200
    And match response.resourceId == 'R-001'
    And match response.date == '2026-02-26'
    And match response.totalQuantity == '#number'
    And match response.availableQuantity == '#number'
    And match response.reservedQuantity == '#number'

  Scenario: OpenAPI required 필드 resourceId 누락 시 400 반환
    Given path basePath + '/getResourceInventory'
    And request { date: '2026-02-26' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 date 누락 시 400 반환
    Given path basePath + '/getResourceInventory'
    And request { resourceId: 'R-001' }
    When method POST
    Then status 400

  Scenario: OpenAPI format 필드 date 잘못된 date 형식 시 400 반환
    Given path basePath + '/getResourceInventory'
    And request { resourceId: 'R-001', date: 'not-a-date' }
    When method POST
    Then status 400
