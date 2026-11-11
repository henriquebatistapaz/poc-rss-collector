---
description: "Autopilot for the cycle: detects where the work stands and RUNS the next step — stopping at human gates"
---

# Next — figure out where we are and move it forward

The user shouldn't have to remember the flow or type spec paths. Detect the state, announce what you're about to do, and **do it** — chaining steps until you hit a human gate or something that needs confirmation.

## 1. Detect the state (facts first)

```bash
git branch --show-current && git status --short | head    # work in progress?
git log --oneline -5
```

- `docs/sdd/INDEX.md` — **the Status column is the state machine** (`Draft` → `Approved` → `In progress` → `Shipped` → `Done`; `Blocked`). Which spec is active and in what status? Any `[NEEDS CLARIFICATION]` in it? Is there a `test-plan.md` next to it? If INDEX.md and reality disagree (e.g. an MR exists but status says `In progress`), fix INDEX.md first — except `Approved`, which only a human sets.
- Latest `/validate-implementation` verdict in this session, if any.
- Open MRs/PRs: `glab mr list` / `gh pr list` — waiting on review? CI red?
- `docs/metrics/`, `docs/tech-debt/` — reports overdue (monthly / quarterly)?
- `.ai-init.json` (or legacy `.ai-start.json` / `.ai-ready.json`) — assets version vs the current ai-init repo tag.

**Which spec is "the" spec:** the one touched by the current branch or the most recently modified under `docs/sdd/`. If two are plausibly active, ask which one — once — instead of guessing.

## 2. Act on the state

Read the corresponding prompt file in `.github/prompts/` and follow it fully — same as if the user had typed it, with the spec path filled in for them.

| State | Action |
|---|---|
| Status `Approved`, no test plan | **Run** `/test-plan` for it |
| Status `Approved` + test plan ready | **Confirm, then run** `/implement-spec` (it sets `In progress`). If `.ai-init.json` has `ai_task` set and the spec is S/M, offer `/delegate` as the alternative in the same question — "implement here, or delegate to the cloud agent?" |
| Spec delegated (open issue citing it with `ai-task-running` or assigned to Copilot, no PR yet) | **Report** "delegated — waiting for the PR" with the issue link; nothing to run here. Run ended with a report and no PR → show the report and offer `/fix-findings` on the spec or implementing here |
| Implementation done, build green, not validated | **Run** `/validate-implementation` |
| Validation NOT READY | **Run** `/fix-findings` for the blockers (ask about `[IMPORTANT]` ones in the same breath), then re-run `/validate-implementation` |
| Validation READY TO SHIP | **Confirm, then run** `/open-mr` (it sets `Shipped`) |
| MR open, CI red | **Run** the `debug-ci-failure` skill and report the classification |
| Status `Draft`, no QA review yet | **Run the QA review YOURSELF**: read `.github/agents/qa.agent.md` and apply its full criteria to the spec — do NOT ask the user to invoke `@qa`. Present the findings + verdict, **always ending with this honesty note, verbatim in spirit**: "Heads-up: I reviewed a spec written in this same session — context bias makes me lean lenient toward my own work. For critical or large specs, invoke `@qa` in a NEW chat for a clean-context, independent review before approving." REVISE SPEC → offer `/fix-findings` for the blockers, then re-review. |
| Status `Draft`, QA verdict SPEC READY | **STOP with the exact unblocking action**: "To approve (Gate 1): set this spec to `Approved` in docs/sdd/INDEX.md — or reply **approved** and I'll do it and continue." If the user replies approved: update INDEX.md, then keep chaining. Stopping here repeatedly is the gate working — never nag, never approve yourself |
| Status `Shipped`, CI green | **STOP with the unblocking action**: "Gate 2: review and merge the MR — after the merge, run /next and I'll mark it `Done` and pick up what's next." |
| Merged (status still `Shipped`) | Set INDEX.md to `Done`, then look for the next source of work |
| Spec has `[NEEDS CLARIFICATION]` | **STOP** — list the open questions for the user |
| The user pasted a production error (stack trace, alert, log) | **Run** `/incident` with it — triage first, containment before analysis on SEV1 |
| Nothing in flight | Offer the sources of work: assigned issues, top `/tech-debt` item, pending `/retro`/`/metrics` recommendation — ask which, then run `/spec-from-issue` or `/new-spec` |
| Reports overdue | Offer to run `/metrics`, `/retro` or `/tech-debt` |
| Assets outdated | Tell the user to run `update.ps1`/`update.sh` |
| A command from AGENTS.md failed, or a path/module it names doesn't exist | Offer `/doctor` — AGENTS.md has likely drifted from the code |

**Chain**: after a step completes, re-evaluate and keep going — spec → test plan → validate → fix → validate → ready — until a STOP or a confirmation point. Cap at 5 chained steps in one run, then summarize where things stand and ask whether to continue.

## Autonomy rules

- **Run without asking**: the QA spec review (via `qa.agent.md` criteria), `/test-plan`, `/validate-implementation`, `/fix-findings` (blockers), `/metrics`, `/tech-debt`, `debug-ci-failure`. They're cheap, reversible, or read-only.
- **"Run @X" always means YOU apply that agent file's criteria** — never instruct the user to switch chat modes; the `.agent.md` files are personas you can load and execute.
- **Ask one short question first**: `/implement-spec` (long and expensive) and `/open-mr` (pushes a branch and creates an MR/PR). State what you'll do and wait for a yes.
- **Never cross a human gate**: never set `Approved` in INDEX.md on your own initiative (only on an explicit "approved" from the user in this conversation), never merge, never apply non-blocking findings without being told, never `--force` anything.
- **Every STOP names the exact human action that unblocks it** — a stop without "here's what you do" is a dead end, and dead ends are bugs.
- Announce before each step in one line ("Spec approved, no test plan → running /test-plan for docs/sdd/specs/20261106-x/spec.md"), so the flow stays visible even when automatic.
- **Model routing**: read the spec's Complexity (S/M/L) and the routing table in AGENTS.md. If the recommended tier for the next step differs from the current model, say so BEFORE running: "This is an L-complexity implementation — recommend switching to <heavy tier> in the model picker; continue here anyway?" (you cannot switch models yourself). For steps where the current model already fits, say nothing — recommend only when the change matters.
- Every step you run still does its own thing — including appending to `usage.log`.

## 3. If the user asks "how does this work?"

Explain compactly: work enters from an issue, an idea or a `/tech-debt`/`/retro` finding; from the spec on it's one funnel (approval → test plan → implement → validate → MR → CI → review → merge); two human gates (spec approval, MR review — see `docs/ONBOARDING.md`); who's who (`@qa` spec, `@reviewer` code, `@architect` design, `@security` for sensitive changes — they point, `/fix-findings` fixes); skills in `.github/skills/` trigger on their own. Point to `AGENTS.md` for commands and `docs/sdd/constitution.md` for the rules.

## Rules

- Ground everything in the state you actually observed — never a generic tour when a specific answer exists.
- One question at a time, max. If you need three answers to proceed, you're guessing too much: report the state and ask the single most blocking one.
- Finish by saying where the work now stands and what happens next (which will often be: "run `/next` again").
