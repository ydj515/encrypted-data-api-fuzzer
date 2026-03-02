# Repository Guidelines

## Project Structure & Module Organization
This repository is a single Gradle-based Spring Boot application. Main code lives under `src/main/java/com/example/mockserver`, with packages split by responsibility such as `controller`, `service`, `dto`, and `exception`. Test code mirrors the main package layout under `src/test/java/com/example/mockserver`. Runtime resources belong in `src/main/resources`. Use `http/mock-rest-api.http` for local request examples and manual API checks.

## Build, Test, and Development Commands
Run commands from the repository root.

- `./gradlew bootRun`: starts the mock REST API server locally on port `18080`.
- `./gradlew test`: runs the JUnit 5 test suite.
- `./gradlew build`: compiles, tests, and assembles the application.
- `./gradlew clean build`: performs a full rebuild when outputs or dependencies are stale.

Always use the Gradle wrapper included in the repository instead of a system Gradle installation.

## Coding Style & Naming Conventions
Target Java 17 and Spring Boot 3.5.x conventions. Use 4-space indentation and UTF-8 source files. Keep class names in `PascalCase`, methods and fields in `camelCase`, and constants in `UPPER_SNAKE_CASE`. Place REST endpoints in `controller`, request/response models in `dto`, business logic in `service`, and shared error handling in `exception`. Prefer small focused methods and constructor-based dependency injection.

## Testing Guidelines
Testing uses `spring-boot-starter-test` with JUnit 5. Name test classes `*Test`, for example `MockApiControllerTest`. Add tests for new endpoints, exception handling, and service-layer branching logic. Run `./gradlew test` before opening a pull request. If behavior changes, update or add request examples in `http/mock-rest-api.http` when useful.

## Commit & Pull Request Guidelines
Recent history follows Conventional Commit prefixes such as `feat:`, `fix:`, `refactor:`, and `docs:`. Keep each commit scoped to one logical change. Pull requests should include a short purpose statement, a summary of key changes, test evidence such as `./gradlew test`, and sample requests or responses when API behavior changes.

## Security & Configuration Tips
Do not commit secrets, tokens, or environment-specific values. Keep local-only overrides out of version control. If configuration is added later, provide safe defaults and document required environment variables in `README.md`.
