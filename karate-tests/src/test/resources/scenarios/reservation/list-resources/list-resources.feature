@service=reservation @api=listResources @kind=single-api
Feature: orgA reservation 자원 목록 조회(본문 기반) 단건 API 테스트

  Background:
    * url gatewayUrl
    * def org = 'orgA'
    * def service = 'reservation'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    Given path basePath + '/listResources'
    And request { page: 0, size: 20, category: 'SPACE' }
    When method POST
    Then status 200
    And match response.items == '#array'
    And match response.page == 0
    And match response.size == 20
    And match response.total == '#number'

  Scenario: OpenAPI range 필드 page 최솟값 미만 시 400 반환
    Given path basePath + '/listResources'
    And request { page: -1, size: 20, category: 'SPACE' }
    When method POST
    Then status 400

  Scenario: OpenAPI range 필드 page 최댓값 초과 시 400 반환
    Given path basePath + '/listResources'
    And request { page: 10001, size: 20, category: 'SPACE' }
    When method POST
    Then status 400

  Scenario: OpenAPI range 필드 size 최솟값 미만 시 400 반환
    Given path basePath + '/listResources'
    And request { page: 0, size: 0, category: 'SPACE' }
    When method POST
    Then status 400

  Scenario: OpenAPI range 필드 size 최댓값 초과 시 400 반환
    Given path basePath + '/listResources'
    And request { page: 0, size: 101, category: 'SPACE' }
    When method POST
    Then status 400

  Scenario: OpenAPI enum 필드 category 허용값 외 값 시 400 반환
    Given path basePath + '/listResources'
    And request { page: 0, size: 20, category: '__INVALID_ENUM__' }
    When method POST
    Then status 400
