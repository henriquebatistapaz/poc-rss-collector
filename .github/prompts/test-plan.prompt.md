---
description: "Derive a layered test plan from an SDD: automated tests per level, manual exploratory script and test data"
---

# Test plan from an SDD

Generate the test plan for: ${input:sdd:Path to the SDD in docs/sdd/ (or an MR/diff, if validating a delivery)}

## Steps

1. Check the spec's status in `docs/sdd/INDEX.md`: it must be **`Approved`**. If it's `Draft`, STOP — Gate 1 hasn't happened; tell the user how to approve (set `Approved` in INDEX.md, or reply "approved"). Then read the SDD, `docs/sdd/constitution.md`, `AGENTS.md` and the test instructions (`.github/instructions/`).
2. For EACH Acceptance Criterion, classify where it is best verified:
   - **Unit** (no infrastructure) — logic, validations, mappings
   - **Integration** (requires database/queue/cache) — persistence, message consumption/production
   - **API/contract** — endpoints: status codes, payload, authentication
   - **Manual exploratory** — what isn't worth automating
3. Add the standard negative scenarios the SDD didn't mention: empty/null/malformed input, external dependency down/timeout, invalid credentials, duplicate message (idempotency), unexpected payload.
4. Produce the plan as the SDD's paired file (`docs/sdd/specs/<name>/test-plan.md`) containing:
   - Table: AC → layer → proposed test name → status (existing/to create)
   - List of additional negative/edge tests
   - **Exploratory script**: numbered manual steps, environment preconditions and expected result
   - **Test data**: concrete payload examples for each scenario (ready to use)
   - What is left out and why
5. If the implementation already exists, compare the plan with the real tests and list the gaps.

## Rules

- No proposed test may call a real external service (LLM, third-party APIs) — always mock/stub.
- Integration tests must declare the required infrastructure.
- A plan without concrete test data is incomplete.

## Usage log — LAST ACTION, ALWAYS

Append ONE line to `docs/sdd/specs/<name>/usage.log` (create if absent). **Append, never rewrite** — prior lines are history:

```bash
echo "$(date -Iseconds) | /test-plan | model: <model used in this chat> | in: ~<input tokens> | out: ~<output tokens>" >> docs/sdd/specs/<name>/usage.log
```

Estimates are fine — mark them with `~`. Input = context sent (files read, history); output = text/code generated. Don't know the model? Ask the user once per session. Do this even if the run was partial or blocked.

## Next step

End your response with: **Next step →** `/implement-spec docs/sdd/specs/<name>/spec.md` (the implementation will follow this plan, tests first).
