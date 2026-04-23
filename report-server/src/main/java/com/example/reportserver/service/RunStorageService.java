package com.example.reportserver.service;

import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestRun;
import com.example.reportserver.model.TestSource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
public class RunStorageService {

    private static final Pattern RUN_ID_PATTERN = Pattern.compile("(?=.*[A-Za-z0-9])[A-Za-z0-9._-]+");

    private final Path dataDir;
    private final ObjectMapper objectMapper;

    @Autowired
    public RunStorageService(@Value("${report.data-dir}") String dataDir, ObjectMapper objectMapper) {
        this(Path.of(dataDir), objectMapper);
    }

    public RunStorageService(Path dataDir, ObjectMapper objectMapper) {
        this.dataDir = dataDir;
        this.objectMapper = objectMapper;
    }

    public void saveRun(TestRun run) {
        Path runDir = runDir(run.getId());
        writeRun(runDir, run);
    }

    public void saveCases(String runId, List<TestCase> cases) {
        Path runDir = runDir(runId);
        writeCases(runDir, cases);
    }

    public void writeRun(Path runDir, TestRun run) {
        try {
            Files.createDirectories(runDir);
            objectMapper.writeValue(runDir.resolve("meta.json").toFile(), run);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeCases(Path runDir, List<TestCase> cases) {
        try {
            Files.createDirectories(runDir);
            objectMapper.writeValue(runDir.resolve("cases.json").toFile(), cases);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Optional<TestRun> findRun(String runId) {
        Path meta = runDir(runId).resolve("meta.json");
        if (!Files.exists(meta)) {
            return Optional.empty();
        }
        try {
            return Optional.of(normalizeReportPath(objectMapper.readValue(meta.toFile(), TestRun.class)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<TestCase> findCases(String runId) {
        Path cases = runDir(runId).resolve("cases.json");
        if (!Files.exists(cases)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(cases.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<TestRun> listAllRuns() {
        Path base = dataDir;
        if (!Files.isDirectory(base)) {
            return Collections.emptyList();
        }
        try (var stream = Files.list(base)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(dir -> dir.resolve("meta.json"))
                    .filter(Files::exists)
                    .flatMap(this::readMeta)
                    .sorted(Comparator.comparing(TestRun::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Stream<TestRun> readMeta(Path metaFile) {
        try {
            return Stream.of(normalizeReportPath(objectMapper.readValue(metaFile.toFile(), TestRun.class)));
        } catch (IOException e) {
            log.warn("meta.json 파싱 실패, 스킵: {}", metaFile, e);
            return Stream.empty();
        }
    }

    private TestRun normalizeReportPath(TestRun run) {
        if (run == null || run.getId() == null || run.getSource() == null) {
            return run;
        }

        String expectedPath = defaultReportPath(run);
        if (expectedPath == null) {
            return run;
        }

        String reportPath = run.getReportPath();
        String legacyPath = "/reports/" + run.getId();
        if (reportPath == null || reportPath.isBlank() || legacyPath.equals(reportPath)) {
            run.setReportPath(expectedPath);
        }
        return run;
    }

    private String defaultReportPath(TestRun run) {
        if (run.getSource() == TestSource.KARATE) {
            return "/reports/" + run.getId() + "/report/karate-summary.html";
        }
        if (run.getSource() == TestSource.CATS) {
            return "/reports/" + run.getId() + "/report/index.html";
        }
        return null;
    }

    public Path baseDir() {
        return dataDir;
    }

    public Path runDir(String runId) {
        return dataDir.resolve(canonicalRunId(runId));
    }

    private String canonicalRunId(String runId) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("Run ID is required");
        }
        String canonicalRunId = runId.trim();
        if (!RUN_ID_PATTERN.matcher(canonicalRunId).matches()) {
            throw new IllegalArgumentException(
                    "Run ID may contain only letters, numbers, dots, underscores, and hyphens: " + runId
            );
        }
        return canonicalRunId;
    }
}
