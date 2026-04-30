# Skill Sync: generate-karate-from-openapi

이 문서는 동일한 스킬을 Agent Skills(`.agents/`)와 Claude Code(`.claude/`) 양쪽에서 유지할 때 싱크를 맞추는 절차를 정의합니다. 어느 쪽에서 수정하든 이 문서를 읽고 대응 항목을 함께 업데이트하세요.

---

## 디렉토리 구조

```text
.agents/skills/generate-karate-from-openapi/
  SKILL.md                          ← Agent Skills frontmatter + 공통 본문
  agents/openai.yaml                ← Agent Skills UI 메타데이터
  evals/
    evals.json                      ← Agent Skills 평가 프롬프트 초안
  references/
    negative-case-generation.md     ← 양쪽 독립 파일. 한쪽 수정 시 다른 쪽도 반영
    project-karate-patterns.md
    scenario-planning.md
  scripts/
    generate_karate_features.py     ← 양쪽 독립 파일. 한쪽 수정 시 다른 쪽도 반영

.claude/skills/generate-karate-from-openapi/
  SKILL.md                          ← Claude Code frontmatter + 공통 본문
  references/
    negative-case-generation.md     ← 양쪽 독립 파일. 한쪽 수정 시 다른 쪽도 반영
    project-karate-patterns.md
    scenario-planning.md
  scripts/
    generate_karate_features.py     ← 양쪽 독립 파일. 한쪽 수정 시 다른 쪽도 반영
```

---

## 섹션 대응 맵

| 섹션 | `.agents/SKILL.md` | `.claude/SKILL.md` | 비고 |
|---|---|---|---|
| frontmatter `name` | `name:` | `name:` | 항상 동일하게 유지 |
| frontmatter `description` | Agent Skills 문법 | Claude Code 문법 | 도구별 독립 편집 가능 |
| frontmatter `compatibility` | 있음 | 없음 | Agent Skills 전용 |
| Overview | Overview | Overview | 내용 동일해야 함 |
| Quick Start | Quick Start | Quick Start | 경로만 각 도구 위치에 맞게 다름 |
| Workflow | Workflow | Workflow | 내용 동일해야 함 |
| Repository Conventions | Repository Conventions | Repository Conventions | 내용 동일해야 함 |
| Script Notes | Script Notes | Script Notes | 내용 동일해야 함 |
| Validation (스킬 구조) | `quick_validate.py` 명령 | 없음 | Agent Skills 전용 |
| Validation (테스트) | `./gradlew test` 예시 | `./gradlew test` 예시 | 내용 동일해야 함 |
| `agents/openai.yaml` | 있음 | 없음 | Agent Skills 전용 |
| `evals/evals.json` | 있음 | 없음 | Agent Skills 평가 입력 관리 |

---

## 변경 시나리오별 체크리스트

### 1. SKILL.md 공통 본문을 수정했을 때

공통 본문 = Overview / Quick Start / Workflow / Repository Conventions / Script Notes / Validation(테스트)

**Claude Code 사용자가 `.claude/skills/.../SKILL.md`를 수정한 경우:**

- [ ] `.agents/skills/generate-karate-from-openapi/SKILL.md`의 동일 섹션을 같은 내용으로 업데이트한다.
- [ ] 두 파일 모두 `> **싱크 안내:**` 블록 인용을 그대로 유지한다.
- [ ] 수정한 섹션이 `핵심 규칙` 목록이면, 규칙 수와 순서가 두 파일에서 일치하는지 확인한다.

**Agent Skills 사용자가 `.agents/skills/.../SKILL.md`를 수정한 경우:**

- [ ] `.claude/skills/generate-karate-from-openapi/SKILL.md`의 동일 섹션을 같은 내용으로 업데이트한다.
- [ ] 두 파일 모두 `> **싱크 안내:**` 블록 인용을 그대로 유지한다.

---

### 2. SKILL.md frontmatter를 수정했을 때

frontmatter는 도구별 독립 항목입니다. 한쪽만 수정해도 됩니다. 단, `name` 필드는 항상 양쪽이 동일해야 합니다.

**`name`을 바꾼 경우:**

- [ ] `.agents/SKILL.md`와 `.claude/SKILL.md` 모두에서 `name:`을 같은 값으로 변경한다.
- [ ] `.agents/agents/openai.yaml`의 `display_name`이 사람이 읽기 좋은 형태로 유지되는지 확인한다.

**`description`만 바꾼 경우:**

- [ ] 해당 도구의 SKILL.md만 수정하면 됩니다. 대응 파일은 수정 불필요.

**`compatibility`를 바꾼 경우:**

- [ ] `.agents/SKILL.md`만 수정하면 됩니다.

---

### 3. `references/*.md`를 수정했을 때

`references/` 파일은 양쪽에 독립적으로 존재합니다. 한쪽을 수정하면 다른 쪽에도 동일 내용을 반영해야 합니다.

**Claude Code 사용자가 `.claude/skills/.../references/<파일명>.md`를 수정한 경우:**

- [ ] `.agents/skills/generate-karate-from-openapi/references/<파일명>.md`에 동일 변경을 적용한다.

**Agent Skills 사용자가 `.agents/skills/.../references/<파일명>.md`를 수정한 경우:**

- [ ] `.claude/skills/generate-karate-from-openapi/references/<파일명>.md`에 동일 변경을 적용한다.

각 참조 파일별 주요 연계 항목:

| 파일 | 연계 확인 항목 |
|---|---|
| `project-karate-patterns.md` | `PATH_TO_API` 표가 바뀌면 `scripts/generate_karate_features.py`의 `PATH_TO_API` 딕셔너리도 확인 |
| `project-karate-patterns.md` | `Stable Test Values` 표가 바뀌면 `scripts/generate_karate_features.py`의 `SAMPLE_VALUES` 딕셔너리도 확인 |
| `negative-case-generation.md` | 새 생성 규칙 추가 시 두 SKILL.md의 Script Notes의 보강 목록도 업데이트 |
| `scenario-planning.md` | 예시(Reservation/Support/Visit) 체인이 바뀌면 두 SKILL.md의 Workflow 4번 항목 설명도 확인 |

---

### 4. `scripts/generate_karate_features.py`를 수정했을 때

스크립트는 양쪽에 독립 파일로 존재합니다. 한쪽을 수정하면 다른 쪽에도 동일 변경을 적용합니다.

**Claude Code 사용자가 `.claude/skills/.../scripts/generate_karate_features.py`를 수정한 경우:**

- [ ] `.agents/skills/generate-karate-from-openapi/scripts/generate_karate_features.py`에 동일 변경을 적용한다.

**Agent Skills 사용자가 `.agents/skills/.../scripts/generate_karate_features.py`를 수정한 경우:**

- [ ] `.claude/skills/generate-karate-from-openapi/scripts/generate_karate_features.py`에 동일 변경을 적용한다.

스크립트 내용 변경 후 추가 확인 항목:

- [ ] 두 SKILL.md의 Quick Start 코드 블록에서 옵션이나 경로가 달라진 게 있으면 양쪽 모두 수정한다.
- [ ] `PATH_TO_API` 딕셔너리가 바뀌었으면 양쪽 `references/project-karate-patterns.md`의 매핑 표도 업데이트한다.
- [ ] `SAMPLE_VALUES` 딕셔너리가 바뀌었으면 양쪽 `references/project-karate-patterns.md`의 `Stable Test Values` 표도 업데이트한다.
- [ ] `--mode` 옵션이 추가/삭제되었으면 두 SKILL.md의 Workflow 4번 항목 설명을 업데이트한다.

---

### 5. `agents/openai.yaml`을 수정했을 때

이 파일은 Agent Skills 전용입니다. Claude Code에 대응 파일이 없으므로 `.agents/` 쪽만 수정하면 됩니다.

- [ ] `display_name`이 변경된 경우, 두 SKILL.md의 `name` frontmatter와 일관성을 유지하는지 확인한다.

---

### 6. `evals/evals.json`을 수정했을 때

이 파일은 Agent Skills에서 평가 프롬프트를 관리하는 용도로 사용합니다.

- [ ] 프롬프트가 현재 저장소 구조와 실제 OpenAPI 파일명을 기준으로 작성되어 있는지 확인한다.
- [ ] 필요하면 [evaluating-skills](https://agentskills.io/skill-creation/evaluating-skills) 가이드에 맞춰 `expected_output`, `assertions`, `files`를 함께 업데이트한다.
- [ ] Claude Code 쪽에서도 동일 평가 세트를 써야 한다면, 별도 복사본을 만들지 말고 이 파일을 기준 데이터로 삼는다.

---

### 7. 새 섹션을 추가했을 때

- [ ] 추가한 섹션이 도구에 무관한 공통 내용이면 양쪽 SKILL.md 모두에 추가한다.
- [ ] 추가한 섹션이 특정 도구 전용이면, 해당 SKILL.md에만 추가하고 이 SKILL-SYNC.md의 `섹션 대응 맵`에 "비고"를 기록한다.

---

## 싱크 검증 방법

두 SKILL.md의 공통 섹션이 동일한지 빠르게 비교:

```bash
# Overview 섹션 비교
diff \
  <(sed -n '/^## Overview/,/^## /p' .agents/skills/generate-karate-from-openapi/SKILL.md | head -n -1) \
  <(sed -n '/^## Overview/,/^## /p' .claude/skills/generate-karate-from-openapi/SKILL.md | head -n -1)

# Workflow 섹션 비교
diff \
  <(sed -n '/^## Workflow/,/^## /p' .agents/skills/generate-karate-from-openapi/SKILL.md | head -n -1) \
  <(sed -n '/^## Workflow/,/^## /p' .claude/skills/generate-karate-from-openapi/SKILL.md | head -n -1)

# Repository Conventions 섹션 비교
diff \
  <(sed -n '/^## Repository Conventions/,/^## /p' .agents/skills/generate-karate-from-openapi/SKILL.md | head -n -1) \
  <(sed -n '/^## Repository Conventions/,/^## /p' .claude/skills/generate-karate-from-openapi/SKILL.md | head -n -1)
```

references 파일 비교:

```bash
diff .agents/skills/generate-karate-from-openapi/references/project-karate-patterns.md \
     .claude/skills/generate-karate-from-openapi/references/project-karate-patterns.md

diff .agents/skills/generate-karate-from-openapi/references/scenario-planning.md \
     .claude/skills/generate-karate-from-openapi/references/scenario-planning.md

diff .agents/skills/generate-karate-from-openapi/references/negative-case-generation.md \
     .claude/skills/generate-karate-from-openapi/references/negative-case-generation.md
```

스크립트 비교:

```bash
diff .agents/skills/generate-karate-from-openapi/scripts/generate_karate_features.py \
     .claude/skills/generate-karate-from-openapi/scripts/generate_karate_features.py
```

스킬 구조 검증:

```bash
python3 "$HOME/.agents/skills/skill-creator/scripts/quick_validate.py" \
  .agents/skills/generate-karate-from-openapi
```

---

## 새 스킬을 추가할 때

1. `.agents/skills/<skill-name>/`에 Agent Skills 구조를 작성한다.
2. `.claude/skills/<skill-name>/SKILL.md`를 Claude Code 포맷으로 작성한다.
3. 공유 참조 파일과 스크립트가 있으면 양쪽 `references/`, `scripts/` 모두에 실제 파일로 복사한다.
4. Agent Skills에서 평가를 돌릴 예정이면 `.agents/skills/<skill-name>/evals/evals.json`을 만든다.
5. 이 문서를 복사하여 `docs/skills/<skill-name>/SKILL-SYNC.md`를 만들고 각 섹션을 해당 스킬에 맞게 수정한다.
6. 각 SKILL.md에 `> **싱크 안내:**` 블록 인용을 추가하여 SKILL-SYNC.md를 가리키게 한다.
