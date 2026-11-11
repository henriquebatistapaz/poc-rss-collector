# Working with AI agents on RSS Collector Mini — read me first

This project uses an AI-assisted development cycle (see AGENTS.md). Agents write most of the code; **your judgment is the product**. This page is about where that judgment matters.

## The cycle and your role in it

```
issue        → /spec-from-issue ┐
idea/request → /new-spec        ├→ spec → [YOU: approve] → /test-plan → /implement-spec
/tech-debt · /retro · /metrics ─┘                       → /validate-implementation
                                                        → /open-mr → [YOU: review] → CI → merge
```

Work starts from wherever it starts — an issue, an idea, a debt-audit finding — but from the spec onward everyone follows the same funnel.

Two human gates. Everything else is automated — which means these two gates are the *only* places your attention protects the codebase.

## Working with the review agents — who, when, and how

**Who reviews what:**

| Agent | Reviews | When |
|---|---|---|
| `@qa` | The SPEC (testability, negative scenarios) | Before your approval — Gate 1 |
| `@reviewer` | The CODE (correctness, security, conventions) | Before/at the MR — Gate 2 |
| `@architect` | The DESIGN (root cause, SOLID, complexity) | Bug fixes and design-heavy changes, at the MR |

Calling `@reviewer` on a spec (or `@qa` on code) isn't harmful, but it's out of turn — each gate has its owner.

**Same-session vs fresh-chat reviews:** `/next` and `/validate-implementation` apply the review agents' criteria automatically, in the same session that produced the work — convenient, but context-biased: an agent leans lenient toward its own output (it will tell you so in its verdict). For critical or large changes, invoke the agent (`@qa`, `@reviewer`, `@architect`) in a **new chat**: clean context, genuinely independent eyes. Rule of thumb — automatic review for routine work, fresh-chat review when getting it wrong is expensive.

**How the interaction works — the three-step loop:**

1. The agent **points** — findings classified `[BLOCKER]`/`[IMPORTANT]`/`[SUGGESTION]`. Review agents never edit anything; that's by design (an editor-reviewer is an author, and the gate disappears).
2. **You decide** which findings to accept — this decision IS your gate. Blockers are mandatory; the rest is judgment.
3. You run `/fix-findings` to **apply** the accepted ones (blockers are mandatory; name the `[IMPORTANT]` ones you also want), then re-run the review agent or `/validate-implementation`.

Every agent and prompt ends by telling you the next step — and `/next` will simply *do* it: it reads the project state and runs the next step (chaining the mechanical ones), stopping at your gates and asking before the expensive ones. Driving manually with the named commands works exactly the same; `/next` is the shortcut.

**Escalations are the system working.** Loops are bounded (3 attempts / 3 review passes — constitution §7): when an agent reports "ESCALATE TO HUMAN" or "same failure after 3 hypotheses", it is doing the right thing — the alternative would be looping forever or quietly weakening a check to converge. Treat an escalation as a decision that reached its rightful owner, not as the agent failing.

## Gate 1 — Approving a spec (the cheap gate)

**How approval is recorded:** the spec's status in `docs/sdd/INDEX.md` — `Draft` → `Approved`. That edit is your signature: make it yourself, or tell the agent "approved" and it updates the INDEX for you. Nothing implements a `Draft` spec (the prompts refuse), and `/next` will keep stopping at it until the status changes — by design.

Don't skim. Defects caught here cost minutes; caught in production they cost days. Check:

- **Acceptance criteria**: could you write a test from each one? "Works correctly" is not an AC.
- **Out of scope**: is what should NOT change written down? Agents over-deliver when scope is open.
- **Negative scenarios**: what happens on bad input, timeout, duplicate event? If only the happy path is specified, only the happy path will be built.
- **`[NEEDS CLARIFICATION]` markers**: every one of them is a question addressed to you. Never approve with unanswered ones.

## Gate 2 — Reviewing an agent MR/PR (the discipline gate)

Agent MRs *look* better than human MRs — clean, consistent, well-described. That polish is exactly why rubber-stamping is dangerous. What to actually do:

- **Read the tests first.** Do they assert behavior, or do they just exercise code? A test that can't fail is decoration. This is the single highest-value check.
- **Check the AC checklist against reality** — open one or two claimed evidences, don't trust the checklist prose.
- **Look for what is NOT in the diff**: missed call sites, docs/wiki pages that should have changed (doc drift), config forgotten.
- **Beware plausible-but-wrong**: agents fail confidently. If a change surprises you, ask — "the agent knew better" is not a review conclusion.
- **Bug fixes: demand the root cause.** A fix that only adds a null check / defensive guard at the crash site is treating the symptom — ask "where does the bad value come from?" (`@architect` and the `fix-root-cause` skill enforce this on the agent side; you enforce it on the approval side).
- **Time check**: if your review took less time than reading the description, you didn't review.

### The AI review comment and the guardrails

- The 🤖 **AI review** comment on the MR/PR (if installed) is a first pass, not an approval. Use its AC → test table as a starting point and open the evidence yourself — the gate is still yours. A `needs-human-review` label means the change touches a sensitive area: read that part line by line.
- **Guardrails** (`.ai-init/hooks/`): run `bash .ai-init/hooks/guard.sh install-git-hook` once per clone. If a rule blocks legitimate work, change `guard.conf` in its own MR — a rule bypassed "just this once" is a rule that no longer exists.

### Labelling an issue `ai-task` (if the automation is installed)

The label starts an unattended implementation of the spec the issue cites — but only of an `Approved` one. That makes **Gate 1 the real control**: approving a spec now means "an agent may build this without anyone watching". Approve only what you would hand to a new colleague with no questions. The PR that comes back gets the same Gate 2 review as any other.

## Warning signs the process is degrading

- You approve specs/MRs without ever requesting changes ← the gate is dead, not the work perfect
- ACs getting vaguer over time
- `/implement-spec` bypassed "just this once" more than once
- Review comments per MR trending to zero (the `/metrics` report tracks this)

## When you may skip the spec

Trivial bug with obvious cause and local fix: failing test that reproduces → fix → `/validate-implementation` → `/open-mr`. Everything else goes through the cycle. "It's just a small feature" is how the cycle dies.

## Improving the system

Correcting the same agent mistake twice? Don't just fix it — that's a missing rule. Run `/retro`, or add it to AGENTS.md/instructions yourself. The assets only get better when friction is fed back into them.
