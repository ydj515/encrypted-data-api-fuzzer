@service=booking @api=getResourceInventory
Feature: 자원 재고 조회

  Background:
    * url gatewayUrl

  Scenario: 재고 조회 성공 — 합계 검증
    Given path basePath + '/getResourceInventory'
    And request { resourceId: 'R-001', date: '2026-04-23' }
    When method POST
    Then status 200
    And match response.resourceId == 'R-001'
    And match response.date == '#string'
    And match response.totalQuantity == '#number'
    And match response.availableQuantity == '#number'
    And match response.reservedQuantity == '#number'
    And assert response.totalQuantity == response.availableQuantity + response.reservedQuantity
    And assert response.availableQuantity >= 0
    And assert response.reservedQuantity >= 0

  Scenario: totalQuantity는 항상 100
    Given path basePath + '/getResourceInventory'
    And request { resourceId: 'R-002', date: '2026-04-23' }
    When method POST
    Then status 200
    And match response.totalQuantity == 100

  Scenario: 존재하지 않는 자원 재고 조회 시 404 반환
    Given path basePath + '/getResourceInventory'
    And request { resourceId: 'R-999', date: '2026-04-23' }
    When method POST
    Then status 404
