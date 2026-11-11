---
description: "Apply the findings from a validation or review: fix at the root, keep the spec in sync, re-run the tests"
---

# Fix findings

Apply the findings from: ${input:findings:The last validation/review (default), or the specific items to apply — e.g. "blockers only", "1 and 3"}

The reviewer points, this prompt fixes. Judging and fixing stay separate on purpose — a gate that fixes its own findings stops being a gate.

## Steps

1. **Collect the findings** — from the latest `/validate-implementation` report or `@reviewer`/`@qa`/`@architect` review in this session. If none is available, ask for the report (or run `/validate-implementation` first). Never invent findings.
2. **Confirm the scope** — `[BLOCKER]` items are mandatory. For `[IMPORTANT]`/`[SUGGESTION]`, apply what the user selected; if they didn't say, list them and ask (one question, not one per item). Anything deliberately skipped gets stated in your summary.
3. **Fix at the root, not at the symptom** — for each finding, ask what actually causes it before editing. If the report names a root cause (e.g. "hardcoded secret; root cause: the enum isn't Spring-managed"), fix THAT, not the surface. Use the `fix-root-cause` skill for anything defect-shaped.
4. **Keep the spec in sync** — findings often expose spec gaps (missing file in the scope section, an unstated requirement). Update `docs/sdd/specs/<name>/spec.md` as part of the fix and flag what changed; the spec is the contract the next validation reads. If the spec change is MATERIAL (scope/ACs changed, not a typo), set its INDEX.md status back to **`Draft`** and tell the user re-approval is needed — silently keeping `Approved` on a changed contract defeats Gate 1.
5. **Tests** — every fixed finding that changed behavior needs a test proving it. A finding about a missing test IS the work: write it.
6. **Run the build+test command** from `AGENTS.md` for the affected modules. Loop budget: if the same failure survives 3 distinct fix attempts, STOP and escalate with what you tried (constitution §7).
7. **Capture the lesson** — for each fixed finding, ask: *would a future agent fall into this same trap in this project?* It qualifies only if it is **project-specific** (names a module, file, command, library quirk or business rule — not generic advice like "write tests"), **causal** (says why it breaks) and **not already covered** by AGENTS.md, the constitution or `.github/instructions/`. One-off typos don't qualify. For each that does, draft one line in the Pitfalls format:
   `- **<area>** — <what breaks and why>. Rule: <what to do instead>. (source: docs/sdd/specs/<name>, <YYYY-MM-DD>)`
   Show the drafts and ask once: "Add these to AGENTS.md → Pitfalls? (yes / no / edit)". Write only what the user approves. If AGENTS.md has no `## Pitfalls` section (installed before it existed), create it right before `## Do not`. If `.ai-init.json` doesn't say `"versioned": true`, AGENTS.md is ignored in the source repository. If it links to an authorized private context repository, tell the user to commit there; otherwise explain that the lesson is local only. Do not force-add it.
   **Already listed?** Then the trap recurred despite the warning — text isn't enough. Don't add a duplicate: append the new source to the existing entry and flag it in the report as a **promotion candidate** (a test, lint rule or guard that enforces it) for the next `/retro`.
8. **Report** — table of finding → what was done → evidence (file/line, test name), plus anything intentionally not applied and why, and the pitfalls added (or proposed and declined).

## Rules

- Never resolve a finding by weakening the check: deleting/skipping a test, broadening a catch, adding a suppression, or softening an acceptance criterion. If a finding seems wrong, say so and leave it open for the human — don't silently discard it.
- Stay inside the scope of the findings. Unrelated improvements you notice go to `/tech-debt`, not into this diff.
- Don't re-run the full validation yourself at the end — that's the next step, and it must be a fresh pass.

## Usage log — LAST ACTION, ALWAYS

Append ONE line to `docs/sdd/specs/<name>/usage.log` (create if absent). **Append, never rewrite** — prior lines are history:

```bash
echo "$(date -Iseconds) | /fix-findings | model: <model used in this chat> | in: ~<input tokens> | out: ~<output tokens>" >> docs/sdd/specs/<name>/usage.log
```

Estimates are fine — mark them with `~`.

## Next step

End your response with: **Next step →** `/validate-implementation docs/sdd/specs/<name>/spec.md` to confirm the blockers are cleared (3rd failed round on the same findings → escalate to a human instead of looping).
