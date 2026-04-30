@service=visit @api=createVisit @kind=single-api
Feature: orgB visit 방문 예약 생성 단건 API 테스트

  Background:
    * url gatewayUrl
    * def org = 'orgB'
    * def service = 'visit'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: 기본 요청 성공
    Given path basePath + '/createVisit'
    And request { siteId: 'SITE-01', visitDate: '2026-02-26', visitorName: 'Kim Visitor', visitorPhone: '010-1234-5678', partySize: 2, purpose: 'demo' }
    When method POST
    Then status 201
    And match response.visitId == '#string'
    And match response.status == 'REQUESTED'
    And match response.createdAt == '#string'
    * def visitId = response.visitId

    # 정리: cancelVisit 호출로 테스트 데이터 상태를 종료
    Given path basePath + '/cancelVisit'
    And request { visitId: '#(visitId)', reason: 'karate generated teardown' }
    When method POST
    Then status 200

  Scenario: OpenAPI required 필드 siteId 누락 시 400 반환
    Given path basePath + '/createVisit'
    And request { visitDate: '2026-02-26', visitorName: 'Kim Visitor', visitorPhone: '010-1234-5678', partySize: 2, purpose: 'demo' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 visitDate 누락 시 400 반환
    Given path basePath + '/createVisit'
    And request { siteId: 'SITE-01', visitorName: 'Kim Visitor', visitorPhone: '010-1234-5678', partySize: 2, purpose: 'demo' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 visitorName 누락 시 400 반환
    Given path basePath + '/createVisit'
    And request { siteId: 'SITE-01', visitDate: '2026-02-26', visitorPhone: '010-1234-5678', partySize: 2, purpose: 'demo' }
    When method POST
    Then status 400

  Scenario: OpenAPI required 필드 partySize 누락 시 400 반환
    Given path basePath + '/createVisit'
    And request { siteId: 'SITE-01', visitDate: '2026-02-26', visitorName: 'Kim Visitor', visitorPhone: '010-1234-5678', purpose: 'demo' }
    When method POST
    Then status 400

  Scenario: OpenAPI format 필드 visitDate 잘못된 date 형식 시 400 반환
    Given path basePath + '/createVisit'
    And request { siteId: 'SITE-01', visitDate: 'not-a-date', visitorName: 'Kim Visitor', visitorPhone: '010-1234-5678', partySize: 2, purpose: 'demo' }
    When method POST
    Then status 400

  Scenario: OpenAPI range 필드 partySize 최솟값 미만 시 400 반환
    Given path basePath + '/createVisit'
    And request { siteId: 'SITE-01', visitDate: '2026-02-26', visitorName: 'Kim Visitor', visitorPhone: '010-1234-5678', partySize: 0, purpose: 'demo' }
    When method POST
    Then status 400

  Scenario: OpenAPI range 필드 partySize 최댓값 초과 시 400 반환
    Given path basePath + '/createVisit'
    And request { siteId: 'SITE-01', visitDate: '2026-02-26', visitorName: 'Kim Visitor', visitorPhone: '010-1234-5678', partySize: 9, purpose: 'demo' }
    When method POST
    Then status 400

  Scenario: OpenAPI enum 필드 purpose 허용값 외 값 시 400 반환
    Given path basePath + '/createVisit'
    And request { siteId: 'SITE-01', visitDate: '2026-02-26', visitorName: 'Kim Visitor', visitorPhone: '010-1234-5678', partySize: 2, purpose: '__INVALID_ENUM__' }
    When method POST
    Then status 400
