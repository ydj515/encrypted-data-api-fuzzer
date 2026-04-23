@service=support @api=listDevices
Feature: orgB support 장비 목록 조회

  Background:
    * url gatewayUrl
    * def org = 'orgB'
    * def service = 'support'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 장비 목록 조회 성공
    Given path basePath + '/listDevices'
    And request { page: 0, size: 20 }
    When method POST
    Then status 200
    And match response.items == '#array'
    And match each response.items == { deviceId: '#string', name: '#string', model: '#string', status: '#string' }

  Scenario: 상태 필터 조회 성공
    Given path basePath + '/listDevices'
    And request { page: 0, size: 20, status: 'ACTIVE' }
    When method POST
    Then status 200
    And match response.items == '#array'
