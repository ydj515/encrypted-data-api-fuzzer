@service=visit @api=getVisitDetail @kind=single-api
Feature: orgB visit 방문 예약 상세 조회(POST 호환) 단건 API 테스트

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

    Given path basePath + '/getVisitDetail'
    And request { visitId: '#(visitId)' }
    When method POST
    Then status 200
    And match response.visitId == visitId
    And match response.siteId == '#string'
    And match response.visitDate == '#string'
    And match response.visitorName == '#string'
    And match response.partySize == '#number'
    And match response.status == '#("REQUESTED" || "CANCELED")'
    And match response.createdAt == '#string'

    # 정리: cancelVisit 호출로 테스트 데이터 상태를 종료
    Given path basePath + '/cancelVisit'
    And request { visitId: '#(visitId)', reason: 'karate generated teardown' }
    When method POST
    Then status 200

  Scenario: OpenAPI required 필드 visitId 누락 시 400 반환
    Given path basePath + '/getVisitDetail'
    And request { }
    When method POST
    Then status 400
