package com.example.reportserver.parser;

import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestRun;
import com.example.reportserver.model.TestSource;
import com.example.reportserver.model.TestStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatsReportParserTest {

    @TempDir
    Path reportDir;

    private CatsReportParser parser;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        parser = new CatsReportParser(objectMapper);
    }

    @Test
    void parseRun_집계_정상() throws Exception {
        writeSummary("""
                {
                  "testCases": [
                    {"id":"Test 1","scenario":"s1","result":"success","path":"/cats/{org}/{service}/createReservation","fuzzer":"F1","httpMethod":"post","httpResponseCode":200,"timeToExecuteInMs":"10"},
                    {"id":"Test 2","scenario":"s2","result":"error","resultDetails":"Not found","path":"/cats/{org}/{service}/createReservation","fuzzer":"F1","httpMethod":"post","httpResponseCode":404,"timeToExecuteInMs":"5"},
                    {"id":"Test 3","scenario":"s3","result":"warn","path":"/cats/{org}/{service}/listResources","fuzzer":"F2","httpMethod":"post","httpResponseCode":200,"timeToExecuteInMs":"3"}
                  ]
                }
                """);

        TestRun run = parser.parseRun(reportDir, "run-1", "catsOrg", "booking", null);

        assertThat(run.getId()).isEqualTo("run-1");
        assertThat(run.getOrg()).isEqualTo("catsOrg");
        assertThat(run.getService()).isEqualTo("booking");
        assertThat(run.getSource()).isEqualTo(TestSource.CATS);
        assertThat(run.getTotalCount()).isEqualTo(3);
        assertThat(run.getPassCount()).isEqualTo(2);  // success + warn
        assertThat(run.getFailCount()).isEqualTo(1);  // error
        assertThat(run.getDurationMs()).isEqualTo(18L);
        assertThat(run.getReportPath()).isEqualTo("/reports/run-1/report/index.html");
    }

    @Test
    void parseCases_케이스_변환() throws Exception {
        writeSummary("""
                {
                  "testCases": [
                    {"id":"Test 1","scenario":"퍼징 케이스 1","result":"success","path":"/cats/{org}/{service}/createReservation","fuzzer":"AbugidasInStringFields","httpMethod":"POST","httpResponseCode":200,"timeToExecuteInMs":"10"},
                    {"id":"Test 2","scenario":"퍼징 케이스 2","result":"error","resultDetails":"404 응답","path":"/cats/{org}/{service}/getResourceInventory","fuzzer":"AcceptHeadersFuzzer","httpMethod":"get","httpResponseCode":404,"timeToExecuteInMs":"5"}
                  ]
                }
                """);

        List<TestCase> cases = parser.parseCases(reportDir, "run-1", "catsOrg", "booking");

        assertThat(cases).hasSize(2);

        TestCase first = cases.get(0);
        assertThat(first.getApi()).isEqualTo("createReservation");
        assertThat(first.getName()).isEqualTo("퍼징 케이스 1");
        assertThat(first.getScenarioName()).isEqualTo("AbugidasInStringFields");
        assertThat(first.getEndpoint()).isEqualTo("/cats/catsOrg/booking/createReservation");
        assertThat(first.getHttpMethod()).isEqualTo("POST");
        assertThat(first.getHttpStatus()).isEqualTo(200);
        assertThat(first.getStatus()).isEqualTo(TestStatus.PASS);
        assertThat(first.getDurationMs()).isEqualTo(10L);
        assertThat(first.getFailureMsg()).isNull();

        TestCase second = cases.get(1);
        assertThat(second.getApi()).isEqualTo("getResourceInventory");
        assertThat(second.getHttpMethod()).isEqualTo("GET");
        assertThat(second.getStatus()).isEqualTo(TestStatus.FAIL);
        assertThat(second.getFailureMsg()).isEqualTo("404 응답");
    }

    @Test
    void parseCases_warn_은_PASS() throws Exception {
        writeSummary("""
                {
                  "testCases": [
                    {"id":"Test 1","scenario":"s1","result":"warn","path":"/cats/{org}/{service}/createReservation","fuzzer":"F","httpMethod":"post","httpResponseCode":200,"timeToExecuteInMs":"1"}
                  ]
                }
                """);

        List<TestCase> cases = parser.parseCases(reportDir, "run-1", "catsOrg", "booking");

        assertThat(cases.get(0).getStatus()).isEqualTo(TestStatus.PASS);
    }

    @Test
    void parseRun_summaryFile이_없으면_예외() {
        assertThatThrownBy(() -> parser.parseRun(reportDir, "run-1", "org", "svc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cats-summary-report.json");
    }

    @Test
    void parseCases_timestamp_TestN_json에서_추출() throws Exception {
        writeSummary("""
                {
                  "testCases": [
                    {"id":"Test 1","scenario":"s1","result":"success","path":"/cats/{org}/{service}/createReservation","fuzzer":"F","httpMethod":"post","httpResponseCode":200,"timeToExecuteInMs":"1"}
                  ]
                }
                """);
        Files.writeString(reportDir.resolve("Test1.json"), """
                {
                  "request": {
                    "timestamp": "Mon, 2 Mar 2026 16:27:16 +0900"
                  }
                }
                """);

        TestRun run = parser.parseRun(reportDir, "run-1", "catsOrg", "booking", null);

        assertThat(run.getStartedAt()).isNotNull();
        assertThat(run.getStartedAt().getYear()).isEqualTo(2026);
        assertThat(run.getStartedAt().getMonthValue()).isEqualTo(3);
        assertThat(run.getStartedAt().getDayOfMonth()).isEqualTo(2);
    }

    private void writeSummary(String json) throws Exception {
        Files.writeString(reportDir.resolve("cats-summary-report.json"), json);
    }
}
