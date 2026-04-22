# Development Guide

## Runtime Environment

This project uses [mise](https://mise.jdx.dev) to pin tool versions across all modules.

| File | Scope |
|---|---|
| `mise.toml` (root) | Java 21.0.2 + aggregate tasks |
| `gateway/mise.toml` | Java 21.0.2 + gateway tasks |
| `mock-rest-api-server/mise.toml` | Java 21.0.2 + mock tasks |

**First-time setup:**
```shell
# From repository root — installs Java 21.0.2 for all modules
mise install
```

If mise is not available, ensure Java 21 is on `PATH` and use the Gradle wrapper directly.

## Mise Tasks

### From the repository root

| Command | Description |
|---|---|
| `mise run gateway:run` | Start gateway service (port 8080) |
| `mise run mock:run` | Start mock-rest-api-server (port 18080) |
| `mise run build` | Build all modules sequentially (gateway → mock) |
| `mise run test` | Run all tests sequentially |
| `mise run clean` | Clean all modules |

Build and test tasks use `set -e` — failure in gateway stops the run before mock is processed.

### From a module directory

```shell
cd gateway
mise run run       # start this service
mise run build     # build this module
mise run test      # test this module
mise run clean     # clean this module
```

Same commands apply inside `mock-rest-api-server/`.

## Running Both Services Together

Gateway and mock server must both be running for end-to-end tests. Open two terminals:

```shell
# terminal 1
mise run mock:run

# terminal 2
mise run gateway:run
```

## Gradle Wrapper (fallback)

If mise is unavailable, run from the module directory:

```shell
cd gateway
./gradlew bootRun     # start
./gradlew test        # test
./gradlew build       # build
./gradlew clean       # clean
```

Always use `./gradlew` (wrapper) rather than a system-level `gradle` installation to ensure consistent Gradle version.
