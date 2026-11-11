---
description: "Implement a feature from an approved SDD, validating against the acceptance criteria"
---

# Implement SDD

Implement the spec: ${input:sdd:Path to the SDD, docs/sdd/specs/<YYYYMMDD>-<slug>/spec.md}

## Steps

1. Check the spec's status in `docs/sdd/INDEX.md`: it must be **`Approved`** (or already `In progress`). `Draft` = REFUSE and explain: Gate 1 approval is recorded by setting `Approved` in INDEX.md — a human edit or an explicit "approved" from the user. Then set the status to **`In progress`** and read the SDD, `docs/sdd/constitution.md` and `AGENTS.md`.
2. If the SDD has `[NEEDS CLARIFICATION]` markers, stop and ask.
3. If a paired test plan exists (`<name>/test-plan.md`), follow it; otherwise suggest running `/test-plan` first.
4. Derive an ordered task list from the SDD's Implementation Plan and present it before coding. Check AGENTS.md → **Pitfalls**: for each entry whose area this spec touches, name it under the task it affects and how you'll follow its rule — a known trap repeated is the most avoidable failure there is.
5. Write the tests derived from the Acceptance Criteria first (they should fail), then implement until they pass — task by task, following the AGENTS.md conventions.
6. Validate with the build+test command from AGENTS.md. Failing test = fix and rerun — **with a loop budget**: if the SAME failure survives 3 distinct fix attempts (different hypotheses, not the same fix retried), STOP. Report: the failure, the 3 hypotheses tried, and your best guess at the real cause. Never "fix" a loop by weakening or deleting the test.
7. Check every Acceptance Criterion of the SDD, one by one, and report the status of each.
8. If a decision diverges from the SDD during implementation, update the SDD and flag the change.
9. **Update affected docs**: if the change alters behavior, flows or structure described in `docs/wiki/` or `AGENTS.md`, update those pages (including mermaid diagrams) as part of the task — doc drift is a bug.

## Rules

- Never consider it done with a failing test or an unmet acceptance criterion.
- Do not add dependencies not foreseen in the SDD without flagging it.

## Usage log — LAST ACTION, ALWAYS

Append ONE line to `docs/sdd/specs/<name>/usage.log` (create if absent). **Append, never rewrite** — prior lines are history:

```bash
echo "$(date -Iseconds) | /implement-spec | model: <model used in this chat> | in: ~<input tokens> | out: ~<output tokens>" >> docs/sdd/specs/<name>/usage.log
```

Estimates are fine — mark them with `~`. Input = context sent (files read, history); output = text/code generated. Don't know the model? Ask the user once per session. Do this even if the run was partial or blocked.

## Next step

When all ACs pass, end your response with: **Next step →** `/validate-implementation docs/sdd/specs/<name>/spec.md` (the quality gate; shipping comes after). If blocked instead, state exactly what is blocking and who can unblock it.
