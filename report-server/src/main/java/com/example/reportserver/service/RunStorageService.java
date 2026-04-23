package com.example.reportserver.service;

import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestRun;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunStorageService {

    @Value("${report.data-dir}")
    private String dataDir;

    private final ObjectMapper objectMapper;

    public void saveRun(TestRun run) {
        Path runDir = runDir(run.getId());
        try {
            Files.createDirectories(runDir);
            objectMapper.writeValue(runDir.resolve("meta.json").toFile(), run);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void saveCases(String runId, List<TestCase> cases) {
        Path runDir = runDir(runId);
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
            return Optional.of(objectMapper.readValue(meta.toFile(), TestRun.class));
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
        Path base = Path.of(dataDir);
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
            return Stream.of(objectMapper.readValue(metaFile.toFile(), TestRun.class));
        } catch (IOException e) {
            log.warn("meta.json 파싱 실패, 스킵: {}", metaFile, e);
            return Stream.empty();
        }
    }

    private Path runDir(String runId) {
        return Path.of(dataDir).resolve(Path.of(runId).getFileName().toString());
    }
}
