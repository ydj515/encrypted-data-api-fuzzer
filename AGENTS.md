# Repository Guidelines

## Project Structure & Module Organization
This repository is a single Gradle module under `gateway/`.
- `gateway/src/main/java/com/example/gateway`: application code (controller, service, config, crypto, util, exception).
- `gateway/src/main/resources`: runtime config templates such as `application-sample.yml`.
- `gateway/src/test/java/com/example/gateway`: unit and Spring context tests mirroring main package paths.
- `gateway/libs`: local JAR dependencies loaded via `fileTree`.

Keep new classes in the closest existing package by responsibility (e.g., routing logic in `service` or `util`, API models in `config` only when config-bound).

## Build, Test, and Development Commands
Run commands from `gateway/`.
- `./gradlew bootRun`: start the Spring Boot app locally.
- `./gradlew test`: run JUnit 5 test suite.
- `./gradlew build`: compile, test, and produce build artifacts.
- `./gradlew clean build`: rebuild from scratch when dependencies or generated outputs are stale.

Use the Gradle wrapper (`./gradlew`) instead of a system Gradle install.

## Coding Style & Naming Conventions
- Refer to `docs/style-guide.md` for detailed style rules.
- Java 17 and Spring Boot 3.x conventions.
- 4-space indentation, UTF-8 source files.
- Class/interface names: `PascalCase`; methods/fields: `camelCase`; constants: `UPPER_SNAKE_CASE`.
- Prefer constructor injection and `final` fields for services.
- Keep methods focused; extract reusable URI/header/crypto logic into dedicated helpers/services.

## Testing Guidelines
- Framework: `spring-boot-starter-test` (JUnit 5).
- Test class naming: `<ClassName>Test` (e.g., `RouteResolverTest`).
- Add tests for new behavior and edge cases, especially route resolution, checksum/encryption flow, and response decryption/error handling.
- Run `./gradlew test` before opening a PR.

## Commit & Pull Request Guidelines
Git history follows Conventional Commit-style prefixes: `feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `docs:`, `build:`.
- Keep commits small and scoped to one logical change.
- PRs should include: purpose, key changes, test evidence (command/results), and related issue/PR links.
- If API behavior changes, include request/response examples.

## Security & Configuration Tips
- Never commit real secrets or production keys.
- Keep sample values in `application-sample.yml`; inject real values via environment variables or secure secret stores.
