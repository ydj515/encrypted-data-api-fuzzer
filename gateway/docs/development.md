# Gateway — Development Guide

For repository-wide setup (mise install, running both services together), see [../../docs/development.md](../../docs/development.md).

## Tasks (from `gateway/` directory)

```shell
mise run run     # start gateway on port 8080
mise run build   # compile, test, package
mise run test    # run JUnit 5 suite
mise run clean   # remove build artifacts
```

## Configuration

Copy the sample config before first run:

```shell
cp src/main/resources/application-sample.yml \
   src/main/resources/application-local.yml
```

Edit `application-local.yml` with real route entries and crypto keys. This file is gitignored.

Activate the local profile:

```shell
SPRING_PROFILES_ACTIVE=local mise run run
# or
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

## Local JAR Dependencies

Custom crypto/checksum implementations live in `libs/`. Gradle picks them up automatically via `fileTree`. To update, replace the JAR file — no `build.gradle.kts` changes needed unless the artifact coordinates change.

## Ports

| Service | Port |
|---|---|
| Gateway | 8080 |
| Mock server (upstream) | 18080 |

Ensure mock-rest-api-server is running before sending end-to-end requests through gateway.
