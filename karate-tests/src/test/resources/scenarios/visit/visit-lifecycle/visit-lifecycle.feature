@service=visit @api=createVisit
Feature: orgB visit 예약 생명주기

  Background:
    * url gatewayUrl
    * def org = 'orgB'
    * def service = 'visit'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 생성 후 조회하고 종료 상태로 변경한다
    Given path basePath + '/createVisit'
    And request { siteId: 'SITE-01', visitDate: '2026-02-26', visitorName: 'Kim Visitor', visitorPhone: '010-1234-5678', partySize: 2, purpose: 'demo' }
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
    And match response.status == 'REQUESTED'

    Given path basePath + '/cancelVisit'
    And request { visitId: '#(visitId)', reason: 'karate generated teardown' }
    When method POST
    Then status 200
    And match response.visitId == visitId
    And match response.status == 'CANCELED'
