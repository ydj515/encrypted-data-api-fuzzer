@service=booking @api=listResources
Feature: 자원 목록 조회

  Background:
    * url gatewayUrl

  Scenario: 기본 목록 조회 성공
    Given path basePath + '/listResources'
    And request { page: 0, size: 20 }
    When method POST
    Then status 200
    And match response.items == '#array'
    And match response.page == 0
    And match response.size == '#number'
    And match response.total == '#number'

  Scenario: 카테고리 필터 조회
    Given path basePath + '/listResources'
    And request { page: 0, size: 20, category: 'SPACE' }
    When method POST
    Then status 200
    And match response.items == '#array'
    And match each response.items == { resourceId: '#string', name: '#string', category: '#string', active: '#boolean' }

  Scenario: size 미전달 시 400 반환
    Given path basePath + '/listResources'
    And request { page: 0 }
    When method POST
    Then status 400
    And match response.code == '#string'
    And match response.message == '#string'
    And match response.traceId == '#string'

  Scenario: 존재하지 않는 서비스 경로는 404 반환
    Given path '/cats/' + org + '/nonexistent/listResources'
    And request { page: 0, size: 20 }
    When method POST
    Then status 404
