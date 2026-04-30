@service=support @api=getTicketDetail @kind=single-api
Feature: orgB support 지원 티켓 상세 조회(POST 호환) 단건 API 테스트

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

    Given path basePath + '/getTicketDetail'
    And request { ticketId: '#(ticketId)' }
    When method POST
    Then status 200
    And match response.ticketId == ticketId
    And match response.deviceId == '#string'
    And match response.requesterId == '#string'
    And match response.issueType == '#string'
    And match response.description == '#string'
    And match response.status == '#("OPEN" || "RESOLVED")'
    And match response.createdAt == '#string'

    # 정리: resolveTicket 호출로 테스트 데이터 상태를 종료
    Given path basePath + '/resolveTicket'
    And request { ticketId: '#(ticketId)', resolutionNote: 'restarted device' }
    When method POST
    Then status 200

  Scenario: OpenAPI required 필드 ticketId 누락 시 400 반환
    Given path basePath + '/getTicketDetail'
    And request { }
    When method POST
    Then status 400
