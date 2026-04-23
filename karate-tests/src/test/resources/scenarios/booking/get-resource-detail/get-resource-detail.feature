@service=booking @api=getResourceDetail
Feature: 자원 상세 조회

  Background:
    * url gatewayUrl

  Scenario: SPACE 카테고리 자원 상세 조회 성공
    Given path basePath + '/getResourceDetail'
    And request { resourceId: 'R-001' }
    When method POST
    Then status 200
    And match response.resourceId == 'R-001'
    And match response.name == '#string'
    And match response.category == 'SPACE'
    And match response.active == true
    And match response.description == '#string'
    And match response.location == '#string'
    And match response.timezone == '#string'

  Scenario: STUDIO 카테고리 자원 상세 조회 성공
    Given path basePath + '/getResourceDetail'
    And request { resourceId: 'R-002' }
    When method POST
    Then status 200
    And match response.resourceId == 'R-002'
    And match response.category == 'STUDIO'

  Scenario: EQUIPMENT 카테고리 자원 상세 조회 성공
    Given path basePath + '/getResourceDetail'
    And request { resourceId: 'R-003' }
    When method POST
    Then status 200
    And match response.resourceId == 'R-003'
    And match response.category == 'EQUIPMENT'

  Scenario: 존재하지 않는 자원 조회 시 404 반환
    Given path basePath + '/getResourceDetail'
    And request { resourceId: 'R-999' }
    When method POST
    Then status 404
