# Repository Guidelines

## Project Structure & Module Organization
This repository is a single Gradle module named `gateway`. Application code lives under `src/main/java/com/example/gateway`, organized by responsibility into packages such as `controller`, `service`, `config`, `crypto`, `util`, and `exception`. Sample runtime configuration belongs in `src/main/resources/application-sample.yml`. Tests mirror the main package layout under `src/test/java/com/example/gateway`. Local JAR dependencies are loaded from `libs/` via Gradle `fileTree`.

## Build, Test, and Development Commands
Always use the Gradle Wrapper.

- `./gradlew bootRun`: run the Spring Boot application locally
- `./gradlew test`: execute the full JUnit 5 test suite
- `./gradlew build`: compile, test, and package the application
- `./gradlew clean build`: rebuild from scratch when cached outputs are stale

At minimum, run `./gradlew test` before and after behavior-changing work.

## Coding Style & Naming Conventions
Follow Java 17 and Spring Boot 3.x conventions. Use 4-space indentation and UTF-8 source files. Class and interface names use `PascalCase`; methods and fields use `camelCase`; constants use `UPPER_SNAKE_CASE`. Prefer constructor injection and `final` fields for services and configuration components. Keep methods focused, and extract reusable routing, header, and crypto logic into dedicated helpers or services.

## Testing Guidelines
Tests use JUnit 5 through `spring-boot-starter-test`. Name test classes with the target class plus `Test`, for example `RouteResolverTest`. Add both happy-path and edge-case coverage for new behavior. Prioritize regression tests around route resolution, header generation, upstream calls, response decryption, and exception response formatting.

## Commit & Pull Request Guidelines
Commit messages follow the Conventional Commit style seen in project history: `feat:`, `fix:`, `refactor:`, `docs:`, `test:`. Keep each commit focused on one logical change. Pull requests should include the purpose, key changes, validation commands run, and whether API behavior changed. If an external interface changes, include request and response examples.

## Security & Configuration Tips
Never commit real secrets, production keys, or sensitive endpoint details. Keep sample values only in `application-sample.yml`, and inject real environment-specific settings through environment variables or a secure secret store.
