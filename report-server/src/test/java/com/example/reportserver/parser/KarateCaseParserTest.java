package com.example.reportserver.parser;

import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestCaseGranularity;
import com.example.reportserver.model.TestCaseType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KarateCaseParserTest {

    @TempDir
    Path reportDir;

    private final KarateCaseParser parser = new KarateCaseParser(new ObjectMapper());

    @Test
    void parseBothScenarioAndHttpCallCases() throws Exception {
        writeFeatureResult();

        List<TestCase> cases = parser.parse(reportDir, "run-1", TestCaseGranularity.BOTH);

        assertThat(cases)
                .extracting(TestCase::getCaseType)
                .containsExactly(TestCaseType.SCENARIO, TestCaseType.HTTP_CALL, TestCaseType.HTTP_CALL);
        assertThat(cases.get(0).getEndpoint()).isEqualTo("MULTIPLE");
        assertThat(cases.get(0).getHttpStatus()).isZero();
        assertThat(cases.get(1).getEndpoint()).isEqualTo("/cats/catsOrg/booking/createReservation");
        assertThat(cases.get(1).getHttpStatus()).isEqualTo(201);
        assertThat(cases.get(2).getEndpoint()).isEqualTo("/cats/catsOrg/booking/getReservationDetail");
        assertThat(cases.get(2).getHttpStatus()).isEqualTo(200);
    }

    @Test
    void parseScenarioOnlyCases() throws Exception {
        writeFeatureResult();

        List<TestCase> cases = parser.parse(reportDir, "run-1", TestCaseGranularity.SCENARIO);

        assertThat(cases).hasSize(1);
        assertThat(cases.getFirst().getCaseType()).isEqualTo(TestCaseType.SCENARIO);
    }

    @Test
    void parseHttpCallOnlyCases() throws Exception {
        writeFeatureResult();

        List<TestCase> cases = parser.parse(reportDir, "run-1", TestCaseGranularity.HTTP_CALL);

        assertThat(cases).hasSize(2);
        assertThat(cases)
                .extracting(TestCase::getCaseType)
                .containsOnly(TestCaseType.HTTP_CALL);
    }

    private void writeFeatureResult() throws Exception {
        Files.writeString(reportDir.resolve("scenarios.booking.create-reservation.karate-json.txt"), """
                {
                  "scenarioResults": [
                    {
                      "name": "reservation lifecycle",
                      "durationMillis": 25.0,
                      "failed": false,
                      "tags": ["service=booking", "api=createReservation"],
                      "stepResults": [
                        {
                          "result": {"millis": 10.0, "status": "passed"},
                          "stepLog": "1 > POST http://localhost:28080/cats/catsOrg/booking/createReservation\\n1 < 201\\n"
                        },
                        {
                          "result": {"millis": 1.0, "status": "passed"}
                        },
                        {
                          "result": {"millis": 8.0, "status": "passed"},
                          "stepLog": "2 > POST http://localhost:28080/cats/catsOrg/booking/getReservationDetail\\n2 < 200\\n"
                        },
                        {
                          "result": {"millis": 1.0, "status": "passed"}
                        }
                      ]
                    }
                  ]
                }
                """);
    }
}
