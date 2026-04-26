@service=support @api=listDevices
Feature: orgB support 장비 목록 조회(본문 기반) 단건 API 테스트

  Background:
    * url gatewayUrl
    * def org = 'orgB'
    * def service = 'support'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    Given path basePath + '/listDevices'
    And request { page: 0, size: 20, status: 'ACTIVE' }
    When method POST
    Then status 200
    And match response.items == '#array'
    And match response.page == 0
    And match response.size == 20
    And match response.total == '#number'

  Scenario: OpenAPI range 필드 page 최솟값 미만 시 400 반환
    Given path basePath + '/listDevices'
    And request { page: -1, size: 20, status: 'ACTIVE' }
    When method POST
    Then status 400

  Scenario: OpenAPI range 필드 page 최댓값 초과 시 400 반환
    Given path basePath + '/listDevices'
    And request { page: 1001, size: 20, status: 'ACTIVE' }
    When method POST
    Then status 400

  Scenario: OpenAPI range 필드 size 최솟값 미만 시 400 반환
    Given path basePath + '/listDevices'
    And request { page: 0, size: 0, status: 'ACTIVE' }
    When method POST
    Then status 400

  Scenario: OpenAPI range 필드 size 최댓값 초과 시 400 반환
    Given path basePath + '/listDevices'
    And request { page: 0, size: 51, status: 'ACTIVE' }
    When method POST
    Then status 400

  Scenario: OpenAPI enum 필드 status 허용값 외 값 시 400 반환
    Given path basePath + '/listDevices'
    And request { page: 0, size: 20, status: '__INVALID_ENUM__' }
    When method POST
    Then status 400
