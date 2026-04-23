package com.example.reportserver.service;

import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestCaseGranularity;
import com.example.reportserver.model.TestRun;
import com.example.reportserver.parser.KarateCaseParser;
import com.example.reportserver.parser.KarateReportParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RunPublishService {

    private static final DateTimeFormatter RUN_ID_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final KarateReportParser karateReportParser;
    private final KarateCaseParser karateCaseParser;
    private final RunStorageService runStorageService;

    public String publishKarate(
            String runId,
            Path reportDir,
            String org,
            String service,
            String api,
            TestCaseGranularity caseGranularity
    ) {
        String effectiveRunId = runId == null || runId.isBlank() ? generateRunId() : runId;
        TestCaseGranularity effectiveGranularity =
                caseGranularity == null ? TestCaseGranularity.BOTH : caseGranularity;
        Path finalRunDir = runStorageService.runDir(effectiveRunId);
        Path tempRunDir = runStorageService.baseDir()
                .resolve("." + effectiveRunId + ".tmp-" + UUID.randomUUID());
        Path stagedReportDir = tempRunDir.resolve("report");

        try {
            if (!Files.isDirectory(reportDir)) {
                throw new IllegalArgumentException("Karate report directory does not exist: " + reportDir);
            }
            if (Files.exists(finalRunDir)) {
                throw new IllegalStateException("Run already exists: " + effectiveRunId);
            }

            Files.createDirectories(runStorageService.baseDir());
            copyDirectory(reportDir, stagedReportDir);

            TestRun run = karateReportParser.parse(
                    stagedReportDir,
                    effectiveRunId,
                    org,
                    service,
                    api,
                    effectiveGranularity
            );
            List<TestCase> cases = karateCaseParser.parse(stagedReportDir, effectiveRunId, effectiveGranularity);

            runStorageService.writeRun(tempRunDir, run);
            runStorageService.writeCases(tempRunDir, cases);
            movePublishedRun(tempRunDir, finalRunDir);

            return effectiveRunId;
        } catch (IOException e) {
            deleteDirectoryIfExists(tempRunDir);
            throw new UncheckedIOException(e);
        } catch (RuntimeException e) {
            deleteDirectoryIfExists(tempRunDir);
            throw e;
        }
    }

    private String generateRunId() {
        return "karate-" + LocalDateTime.now().format(RUN_ID_FORMATTER) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.forEach(source -> copyPath(sourceDir, source, targetDir));
        }
    }

    private void copyPath(Path sourceDir, Path source, Path targetDir) {
        Path target = targetDir.resolve(sourceDir.relativize(source).toString());
        try {
            if (Files.isDirectory(source)) {
                Files.createDirectories(target);
            } else {
                Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void movePublishedRun(Path tempRunDir, Path finalRunDir) throws IOException {
        try {
            Files.move(tempRunDir, finalRunDir, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempRunDir, finalRunDir);
        }
    }

    private void deleteDirectoryIfExists(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(this::deletePath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void deletePath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
