package karate;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KarateRunner {

    @Test
    void testAll() {
        String service = System.getenv("SERVICE");
        String api = System.getenv("API");
        List<String> tags = buildTagFilter(service, api);
        var builder = Runner.path("classpath:scenarios");
        if (!tags.isEmpty()) {
            builder.tags(tags);
        }

        Results results = builder.parallel(5);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }

    private List<String> buildTagFilter(String service, String api) {
        var parts = new ArrayList<String>();
        String effectiveService = (service == null || service.isBlank()) ? "reservation" : service;
        parts.add("@service=" + effectiveService);
        if (api != null && !api.isBlank()) {
            parts.add("@api=" + api);
        }
        return parts;
    }
}
