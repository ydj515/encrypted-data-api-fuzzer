@service=visit @api=siteVisitFlow @kind=scenario @scenario=llm-data-flow
Feature: orgB visit site 응답 기반 방문 예약 흐름

  Background:
    * url gatewayUrl
    * def org = 'orgB'
    * def service = 'visit'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: site 목록 응답의 siteId로 slot을 확인하고 방문 예약을 취소한다
    Given path basePath + '/listSites'
    And request { page: 0, size: 20, city: 'Seoul' }
    When method POST
    Then status 200
    And match response.items == '#[_ > 0]'
    * def siteId = response.items[0].siteId
    * def visitDate = '2026-02-26'

    Given path basePath + '/getSiteSlots'
    And request { siteId: '#(siteId)', date: '#(visitDate)' }
    When method POST
    Then status 200
    And match response.siteId == siteId
    And assert response.availableSlots > 0

    Given path basePath + '/createVisit'
    And request { siteId: '#(siteId)', visitDate: '#(visitDate)', visitorName: 'LLM Visitor', visitorPhone: '010-2222-3333', partySize: 2, purpose: 'demo' }
    When method POST
    Then status 201
    And match response.visitId == '#string'
    And match response.status == 'REQUESTED'
    * def visitId = response.visitId

    Given path basePath + '/getVisitDetail'
    And request { visitId: '#(visitId)' }
    When method POST
    Then status 200
    And match response.visitId == visitId
    And match response.siteId == siteId
    And match response.visitDate == visitDate
    And match response.status == 'REQUESTED'

    Given path basePath + '/cancelVisit'
    And request { visitId: '#(visitId)', reason: 'llm data-flow teardown' }
    When method POST
    Then status 200
    And match response.visitId == visitId
    And match response.status == 'CANCELED'
