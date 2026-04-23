package com.example.reportserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "report.data-dir=${java.io.tmpdir}/report-server-test-runs")
class ReportServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
