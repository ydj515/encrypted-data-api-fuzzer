@service=visit @api=getSiteSlots
Feature: orgB visit site slot 조회

  Background:
    * url gatewayUrl
    * def org = 'orgB'
    * def service = 'visit'
    * def basePath = '/cats/' + org + '/' + service

  Scenario: site slot 조회 성공
    Given path basePath + '/getSiteSlots'
    And request { siteId: 'SITE-01', date: '2026-02-26' }
    When method POST
    Then status 200
    And match response.siteId == 'SITE-01'
    And match response.totalSlots == '#number'
    And match response.availableSlots == '#number'
    And match response.reservedSlots == '#number'
    And assert response.totalSlots == response.availableSlots + response.reservedSlots

  Scenario: 존재하지 않는 site 조회 시 404 반환
    Given path basePath + '/getSiteSlots'
    And request { siteId: 'SITE-99', date: '2026-02-26' }
    When method POST
    Then status 404
