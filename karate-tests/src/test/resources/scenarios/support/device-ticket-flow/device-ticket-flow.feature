@service=support @api=deviceTicketFlow @kind=scenario @scenario=llm-data-flow
Feature: orgB support 장비 응답 기반 티켓 처리 흐름

  Background:
    * url gatewayUrl
    * def org = 'orgB'
    * def service = 'support'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 장비 목록 응답의 deviceId로 티켓을 생성하고 해결한다
    Given path basePath + '/listDevices'
    And request { page: 0, size: 20, status: 'ACTIVE' }
    When method POST
    Then status 200
    And match response.items == '#[_ > 0]'
    * def deviceId = response.items[0].deviceId

    Given path basePath + '/createTicket'
    And request { deviceId: '#(deviceId)', requesterId: 'llm-flow-ops-user', issueType: 'DISPLAY_ERROR', description: 'llm data-flow support ticket' }
    When method POST
    Then status 201
    And match response.ticketId == '#string'
    And match response.status == 'OPEN'
    * def ticketId = response.ticketId

    Given path basePath + '/getTicketDetail'
    And request { ticketId: '#(ticketId)' }
    When method POST
    Then status 200
    And match response.ticketId == ticketId
    And match response.deviceId == deviceId
    And match response.requesterId == 'llm-flow-ops-user'
    And match response.status == 'OPEN'

    Given path basePath + '/resolveTicket'
    And request { ticketId: '#(ticketId)', resolutionNote: 'llm data-flow resolved' }
    When method POST
    Then status 200
    And match response.ticketId == ticketId
    And match response.status == 'RESOLVED'
