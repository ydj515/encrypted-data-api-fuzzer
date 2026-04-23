@service=booking @api=createReservation
Feature: 예약 전체 생명주기 체인 테스트

  Background:
    * url gatewayUrl

  Scenario: 예약 전체 생명주기
    # 1. 예약 생성 (createReservation → 201)
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001', scheduleId: 'R-001-2026-02-26-9', userId: 'user-karate-001', quantity: 1, memo: 'karate lifecycle test' }
    When method POST
    Then status 201
    And match response.reservationId == '#string'
    And match response.status == '#("CREATED" || "CONFIRMED" || "WAITLISTED")'
    And match response.createdAt == '#string'
    * def reservationId = response.reservationId

    # 2. 예약 상세 조회 (getReservationDetail → 200)
    Given path basePath + '/getReservationDetail'
    And request { reservationId: '#(reservationId)' }
    When method POST
    Then status 200
    And match response.reservationId == reservationId
    And match response.resourceId == 'R-001'
    And match response.scheduleId == 'R-001-2026-02-26-9'
    And match response.userId == 'user-karate-001'
    And match response.quantity == 1
    And match response.status == '#("CREATED" || "CONFIRMED" || "WAITLISTED")'
    And match response.createdAt == '#string'

    # 3. 예약 취소 (cancelReservation → 200), 취소 후 status == 'CANCELED' 검증
    Given path basePath + '/cancelReservation'
    And request { reservationId: '#(reservationId)', reason: 'karate lifecycle teardown' }
    When method POST
    Then status 200
    And match response.reservationId == reservationId
    And match response.status == 'CANCELED'
    And match response.canceledAt == '#string'

  Scenario: 취소된 예약 재조회
    # 1. 예약 생성
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001', scheduleId: 'R-001-2026-02-26-9', userId: 'user-karate-001', quantity: 1, memo: 'karate recheck test' }
    When method POST
    Then status 201
    And match response.reservationId == '#string'
    * def reservationId = response.reservationId

    # 2. 예약 취소
    Given path basePath + '/cancelReservation'
    And request { reservationId: '#(reservationId)', reason: 'karate recheck teardown' }
    When method POST
    Then status 200
    And match response.status == 'CANCELED'

    # 3. 취소된 예약 재조회 (getReservationDetail → 200), status == 'CANCELED' 확인
    Given path basePath + '/getReservationDetail'
    And request { reservationId: '#(reservationId)' }
    When method POST
    Then status 200
    And match response.reservationId == reservationId
    And match response.status == 'CANCELED'
    And match response.canceledAt == '#string'

  Scenario: 필수 필드 누락 시 400 반환
    # resourceId만 전달하여 scheduleId, userId 등 누락 → 400 기대
    Given path basePath + '/createReservation'
    And request { resourceId: 'R-001' }
    When method POST
    Then status 400
