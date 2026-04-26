@service=support @api=createTicket
Feature: orgB support 티켓 생명주기

  Background:
    * url gatewayUrl
    * def org = 'orgB'
    * def service = 'support'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 생성 후 조회하고 종료 상태로 변경한다
    Given path basePath + '/createTicket'
    And request { deviceId: 'DEV-01', requesterId: 'ops-user', issueType: 'DISPLAY_ERROR', description: 'screen is blank' }
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
    And match response.status == 'OPEN'

    Given path basePath + '/resolveTicket'
    And request { ticketId: '#(ticketId)', resolutionNote: 'restarted device' }
    When method POST
    Then status 200
    And match response.ticketId == ticketId
    And match response.status == 'RESOLVED'
