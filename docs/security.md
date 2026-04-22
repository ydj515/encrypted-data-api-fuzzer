# Security & Configuration

## General Rules

- **Never commit real secrets** — no production keys, API keys, tokens, or passwords in any tracked file.
- **Use `application-sample.yml`** for config templates with placeholder values.
- Inject real values via environment variables or a secure secret store (e.g., Vault, AWS Secrets Manager).

## Local Configuration

Copy the sample config and override locally (keep the override out of git):

```shell
cp gateway/src/main/resources/application-sample.yml \
   gateway/src/main/resources/application-local.yml
# edit application-local.yml with real values
# application-local.yml is .gitignored
```

Run with the local profile:
```shell
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

## Sensitive Data in Logs

- Do not log encryption keys, plaintext data, or full ciphertext.
- Log only non-sensitive identifiers: `org`, `service`, `api`, target URL.
- If a value must appear partially for debugging, log a short prefix only.

## Crypto Module

The default `CryptoModule` and `ChecksumModule` implementations (Base64 + SHA-256) are for local development only. Production implementations must be loaded via the `libs/` JAR or a Spring profile. Do not ship stub implementations behind a feature flag — use Spring profiles to replace them cleanly.

## .gitignore Reminders

Ensure these patterns are covered:

```
application-local.yml
application-*.yml  # except application-sample.yml
*.key
*.pem
.env
```
