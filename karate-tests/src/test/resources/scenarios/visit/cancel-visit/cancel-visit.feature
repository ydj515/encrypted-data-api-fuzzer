@service=visit @api=cancelVisit @kind=single-api
Feature: orgB visit 방문 예약 취소 단건 API 테스트

  Background:
    * url gatewayUrl
    * def org = 'orgB'
    * def service = 'visit'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    # 사전 조건: createVisit 응답 필드 visitId 값을 요청에 사용
    Given path basePath + '/createVisit'
    And request { siteId: 'SITE-01', visitDate: '2026-02-26', visitorName: 'Kim Visitor', visitorPhone: '010-1234-5678', partySize: 2, purpose: 'demo' }
    When method POST
    Then status 201
    And match response.visitId == '#string'
    * def visitId = response.visitId

    Given path basePath + '/cancelVisit'
    And request { visitId: '#(visitId)', reason: 'karate generated teardown' }
    When method POST
    Then status 200
    And match response.visitId == visitId
    And match response.status == 'CANCELED'
    And match response.canceledAt == '#string'

  Scenario: OpenAPI required 필드 visitId 누락 시 400 반환
    Given path basePath + '/cancelVisit'
    And request { reason: 'karate generated teardown' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 reason 누락 시 400 반환
    Given path basePath + '/cancelVisit'
    And request { visitId: 'VIS-550e8400-e29b-41d4-a716-446655440002' }
    When method POST
    Then status 400
