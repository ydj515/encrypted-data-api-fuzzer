package com.example.reportserver.parser;

import com.example.reportserver.model.TestCaseGranularity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KarateReportParserTest {

    @TempDir
    Path reportDir;

    private final KarateReportParser parser = new KarateReportParser(new ObjectMapper());

    @Test
    void failWhenSummaryResultDateIsInvalid() throws Exception {
        Files.writeString(reportDir.resolve("karate-summary-json.txt"), """
                {
                  "resultDate": "invalid-date",
                  "elapsedTime": 42.0,
                  "scenariosPassed": 1,
                  "scenariosfailed": 0
                }
                """);

        assertThatThrownBy(() -> parser.parse(
                reportDir,
                "run-1",
                "catsOrg",
                "booking",
                null,
                TestCaseGranularity.BOTH
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Karate summary resultDate");
    }
}
