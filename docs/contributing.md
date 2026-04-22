# Commit & PR Guidelines

## Commit Message Format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <short description>
```

| Type | When to use |
|---|---|
| `feat` | New feature or behavior |
| `fix` | Bug fix |
| `refactor` | Code change with no behavior change |
| `test` | Adding or updating tests |
| `docs` | Documentation only |
| `build` | Build scripts, dependencies, Gradle config |
| `chore` | Maintenance (mise, CI config, etc.) |

Keep each commit scoped to one logical change. Avoid mixing refactors with feature changes.

## Pull Request Checklist

A PR description should include:

- **Purpose** — what problem does this solve?
- **Key changes** — bullet list of what changed and why.
- **Test evidence** — command run + outcome (e.g., `mise run test` → all pass).
- **API changes** — if the external interface changed, include request/response examples.
- **Related issues/PRs** — link if applicable.

## Before Opening a PR

```shell
mise run test    # must pass
mise run build   # must succeed
```

If adding a new API route or changing encryption behavior, update `docs/architecture.md` and `application-sample.yml` as needed.
