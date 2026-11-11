---
description: "Create a spec through a guided interview: explore the codebase, ask what matters, draft-and-confirm — for work that doesn't come from an issue"
---

# New spec — interview mode

Starting point: ${input:feature:One line on what you want to build/change (rough is fine — the interview sharpens it)}

A robust spec is extracted, not dictated: the user has the intent, the codebase has the constraints, and your job is to bring both together. Interview first, write after.

## Phase 1 — Ground yourself before asking anything

1. Read `docs/sdd/constitution.md`, `docs/sdd/TEMPLATE.sdd.md`, `AGENTS.md` and skim `docs/wiki/` (or the architecture docs).
2. **Explore the code the feature will touch** — find the modules, entities and integration points involved. Questions grounded in the real code ("this will touch the `X` consumer — should the new field flow through to `Y`?") are worth ten generic ones.
3. If the idea conflicts with the constitution, raise it now and stop.
4. Check AGENTS.md → `## Pitfalls` for entries in the areas this feature touches — carry them into the spec's Risks section (and into an acceptance criterion when the trap is testable).

## Phase 2 — The interview

Work through these areas **in rounds of at most 2-3 related questions** — never a 15-question wall. Skip what the user already answered or the code already answers; go deeper where answers come back vague.

| Area | What you're extracting |
|---|---|
| Problem & value | WHY this, why now — what breaks or is lost without it? Who is affected? |
| Outcome | What does "done and working" look like, concretely, from the user's seat? |
| Scope boundaries | What is explicitly OUT? (Agents over-deliver when scope is open — force this one.) |
| Behavior details | The decisions the code will need: edge inputs, ordering, duplicates, limits, permissions |
| Negative scenarios | What should happen on bad input / dependency down / timeout / duplicate event? |
| NFRs | Volume, latency, cost ceilings — only what's real for this feature, not a checklist |
| Integration | Which existing flows/contracts does it touch (from Phase 1)? Data migration needed? |
| Risks | What could make this blow up or drag on? Anything irreversible? |

**Technique — draft, don't interrogate:** whenever you can infer a plausible answer from the code or context, PROPOSE it and ask for correction ("I'd put the mobile API out of scope — confirm?"). Correcting is easier than authoring. Accept "I don't know" as an answer — that's what `[NEEDS CLARIFICATION]` markers are for; don't stall the interview on it.

**Stop when marginal questions stop changing the spec** — typically one round for S complexity, 2-3 for M, more for L. Over-interviewing kills the tool.

## Phase 3 — Write

1. Draft `docs/sdd/specs/<YYYYMMDD>-<kebab-slug>/spec.md` (`<YYYYMMDD>` = today, `date +%Y%m%d`, fixed at creation — the directory is never renamed; `<kebab-slug>` = short kebab-case name; `<name>` below means this whole `<YYYYMMDD>-<kebab-slug>` directory name) filling every template section — including the **Complexity** field (S/M/L per the rubric; justify in half a line — it drives model routing later) — from the interview answers and Phase 1 facts. Decide **Security-sensitive** (yes if it touches a `SENSITIVE` glob in `.ai-init/hooks/guard.conf` or handles auth, authorization, personal data, payments, secrets/crypto, uploads, outbound URLs or infra); if yes, fill section 9.1 Threats concretely and turn every testable mitigation into an AC. Acceptance criteria must be testable; unresolved points become `[NEEDS CLARIFICATION]`.
2. Present the spec with a 5-line summary on top: problem, outcome, in/out of scope, the ACs, what's still open. The user reacts to the summary; the spec is the record.
3. Register it in `docs/sdd/INDEX.md` with status **`Draft`** (the INDEX Status column is the cycle's state machine — see the legend there).

Do NOT create a per-spec prompt file — the generic cycle prompts (`/test-plan`, `/implement-spec`, `/validate-implementation`, `/open-mr`) take the spec path as input. A dedicated prompt is only justified for RECURRING procedures (runbooks, deploy guides), not one-shot feature specs.

## Rules

- The spec describes WHAT and WHY. HOW decisions go in the Architecture section, always justified.
- Never invent an answer the user didn't give and the code doesn't show — ask, or mark `[NEEDS CLARIFICATION]`.
- One round of questions at a time; wait for the answers before the next round.
- Do not start implementation — the spec needs human approval first.

## Usage log — LAST ACTION, ALWAYS

Append ONE line to `docs/sdd/specs/<name>/usage.log` (create if absent). **Append, never rewrite** — prior lines are history:

```bash
echo "$(date -Iseconds) | /new-spec | model: <model used in this chat> | in: ~<input tokens> | out: ~<output tokens>" >> docs/sdd/specs/<name>/usage.log
```

Estimates are fine — mark them with `~`. Input = context sent (files read, history); output = text/code generated. Don't know the model? Ask the user once per session. Do this even if the run was partial or blocked.

## Next step

End your response with: **Next step →** run `/next` — it performs the QA review automatically (applying `.github/agents/qa.agent.md`) and then waits for YOUR approval (Gate 1). **Approving = setting the spec's status to `Approved` in `docs/sdd/INDEX.md`** — edit it, or reply "approved" and it gets recorded for you. Until then the cycle stops there — that's the gate working, not a loop. (Optional: invoke `@qa` in a fresh chat for a more independent review.)
