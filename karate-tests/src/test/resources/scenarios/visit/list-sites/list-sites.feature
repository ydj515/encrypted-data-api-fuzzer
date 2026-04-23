@service=visit @api=listSites
Feature: orgB visit site 목록 조회

  Background:
    * url gatewayUrl
    * def org = 'orgB'
    * def service = 'visit'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 site 목록 조회 성공
    Given path basePath + '/listSites'
    And request { page: 0, size: 20 }
    When method POST
    Then status 200
    And match response.items == '#array'
    And match each response.items == { siteId: '#string', name: '#string', city: '#string', active: '#boolean' }

  Scenario: city 필터 조회 성공
    Given path basePath + '/listSites'
    And request { page: 0, size: 20, city: 'Seoul' }
    When method POST
    Then status 200
    And match response.items == '#array'
