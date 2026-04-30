@service=reservation @api=getResourceDetail @kind=single-api
Feature: orgA reservation 자원 상세 조회(POST 호환) 단건 API 테스트

  Background:
    * url gatewayUrl
    * def org = 'orgA'
    * def service = 'reservation'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    Given path basePath + '/getResourceDetail'
    And request { resourceId: 'R-001' }
    When method POST
    Then status 200
    And match response.resourceId == 'R-001'
    And match response.name == '#string'
    And match response.category == '#("SPACE" || "STUDIO" || "EQUIPMENT")'
    And match response.active == '#boolean'
    And match response.description == '#string'
    And match response.location == '#string'
    And match response.timezone == '#string'

  Scenario: OpenAPI required 필드 resourceId 누락 시 400 반환
    Given path basePath + '/getResourceDetail'
    And request { }
    When method POST
    Then status 400

  Scenario: OpenAPI minLength 필드 resourceId 최소 길이 미만 시 400 반환
    Given path basePath + '/getResourceDetail'
    And request { resourceId: '' }
    When method POST
    Then status 400

  Scenario: OpenAPI maxLength 필드 resourceId 최대 길이 초과 시 400 반환
    Given path basePath + '/getResourceDetail'
    And request { resourceId: 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX' }
    When method POST
    Then status 400
