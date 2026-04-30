@service=support @api=resolveTicket @kind=single-api
Feature: orgB support 지원 티켓 해결 처리 단건 API 테스트

  Background:
    * url gatewayUrl
    * def org = 'orgB'
    * def service = 'support'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    # 사전 조건: createTicket 응답 필드 ticketId 값을 요청에 사용
    Given path basePath + '/createTicket'
    And request { deviceId: 'DEV-01', requesterId: 'ops-user', issueType: 'DISPLAY_ERROR', description: 'screen is blank' }
    When method POST
    Then status 201
    And match response.ticketId == '#string'
    * def ticketId = response.ticketId

    Given path basePath + '/resolveTicket'
    And request { ticketId: '#(ticketId)', resolutionNote: 'restarted device' }
    When method POST
    Then status 200
    And match response.ticketId == ticketId
    And match response.status == 'RESOLVED'
    And match response.resolvedAt == '#string'

  Scenario: OpenAPI required 필드 ticketId 누락 시 400 반환
    Given path basePath + '/resolveTicket'
    And request { resolutionNote: 'restarted device' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 resolutionNote 누락 시 400 반환
    Given path basePath + '/resolveTicket'
    And request { ticketId: 'TCK-550e8400-e29b-41d4-a716-446655440001' }
    When method POST
    Then status 400
