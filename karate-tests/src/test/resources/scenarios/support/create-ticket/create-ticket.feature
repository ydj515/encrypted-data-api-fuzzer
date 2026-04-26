@service=support @api=createTicket
Feature: orgB support 지원 티켓 생성 단건 API 테스트

  Background:
    * url gatewayUrl
    * def org = 'orgB'
    * def service = 'support'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    Given path basePath + '/createTicket'
    And request { deviceId: 'DEV-01', requesterId: 'ops-user', issueType: 'DISPLAY_ERROR', description: 'screen is blank' }
    When method POST
    Then status 201
    And match response.ticketId == '#string'
    And match response.status == 'OPEN'
    And match response.createdAt == '#string'
    * def ticketId = response.ticketId

    # 정리: resolveTicket 호출로 테스트 데이터 상태를 종료
    Given path basePath + '/resolveTicket'
    And request { ticketId: '#(ticketId)', resolutionNote: 'restarted device' }
    When method POST
    Then status 200

  Scenario: OpenAPI required 필드 deviceId 누락 시 400 반환
    Given path basePath + '/createTicket'
    And request { requesterId: 'ops-user', issueType: 'DISPLAY_ERROR', description: 'screen is blank' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 requesterId 누락 시 400 반환
    Given path basePath + '/createTicket'
    And request { deviceId: 'DEV-01', issueType: 'DISPLAY_ERROR', description: 'screen is blank' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 issueType 누락 시 400 반환
    Given path basePath + '/createTicket'
    And request { deviceId: 'DEV-01', requesterId: 'ops-user', description: 'screen is blank' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 description 누락 시 400 반환
    Given path basePath + '/createTicket'
    And request { deviceId: 'DEV-01', requesterId: 'ops-user', issueType: 'DISPLAY_ERROR' }
    When method POST
    Then status 400

  Scenario: OpenAPI enum 필드 issueType 허용값 외 값 시 400 반환
    Given path basePath + '/createTicket'
    And request { deviceId: 'DEV-01', requesterId: 'ops-user', issueType: '__INVALID_ENUM__', description: 'screen is blank' }
    When method POST
    Then status 400
