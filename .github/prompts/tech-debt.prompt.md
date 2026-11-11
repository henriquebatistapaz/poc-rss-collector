---
description: "Audit the project's technical debt: find it, classify it by interest rate, and turn the worst items into SDD candidates"
---

# Technical debt audit

Audit scope: ${input:scope:Whole project, or a specific module/path}

Debt is not "code I dislike" — it is something that **slows or endangers future work**. Every finding needs evidence and a stated cost.

## 1. Collect signals (facts, not vibes)

- **Harness data**: current coverage vs floor (per module — averages hide holes), lint/type-checker suppressions (`@SuppressWarnings`, `# noqa`, `# type: ignore` counts), skipped/quarantined tests.
- **Code markers**: `grep -rn "TODO\|FIXME\|HACK\|XXX"` — with age (`git log -1 --format=%cr` per file); a 3-year-old TODO is a decision, not a task.
- **Dependency staleness**: versions vs latest (`mvn versions:display-dependency-updates` / `pip list --outdated`), EOL frameworks, known CVEs.
- **Change–pain hotspots**: files that change most (`git log --format= --name-only | sort | uniq -c | sort -rn | head -20`) intersected with the largest/most complex files — debt in a hotspot charges interest on every MR; debt in a frozen corner charges almost none.
- **Design smells in hotspots** (focus there, not everywhere): god classes, deep nesting, duplicated logic across modules, layers bypassed (controller touching repository directly, etc. per AGENTS.md conventions).
- **Doc/spec drift**: wiki/AGENTS.md claims contradicted by code; SDDs marked in-progress but abandoned.
- **Bug clustering**: modules that keep producing fix-MRs (via `glab`/`gh` history) — recurring symptom fixes suggest an un-fixed root cause (see `fix-root-cause`).

## 2. Classify — principal vs interest

For each item: **what it is** (evidence: file/path/number) · **type** (design / test / dependency / doc / infra) · **interest** (how it taxes work: "every new provider copies this duplication", "this module can't be tested without full docker-compose") · **principal** (rough cost to fix: S/M/L) · **risk of fixing** (touching it safely needs which tests?).

High interest + low principal = do now. High + high = SDD. Low interest = record and consciously ignore — a debt register full of trivia hides the dangerous items.

## 3. Report

Produce `docs/tech-debt/<YYYY-MM>.md`: top items ranked by interest (each with evidence and cost), the ignored-on-purpose list, and a comparison with the previous audit if one exists (what was paid, what grew). For the top 2-3: draft SDD titles ready for `/new-spec`.

## Rules

- Evidence-based only — every item cites files/numbers. "This module is messy" is not a finding.
- Hotspot-weighted: ugly-but-frozen code ranks below mildly-annoying-but-touched-weekly code.
- No fixing during the audit — this prompt produces a map, not changes. Paying debt goes through the normal cycle (issue → SDD → implement).
- Not people metrics: no per-author attribution, ever.

## Usage log — LAST ACTION, ALWAYS

Append ONE line to `docs/sdd/usage.log` (create if absent). **Append, never rewrite** — prior lines are history:

```bash
echo "$(date -Iseconds) | /tech-debt | model: <model used in this chat> | in: ~<input tokens> | out: ~<output tokens>" >> docs/sdd/usage.log
```

Estimates are fine — mark them with `~`.

## Next step

End your response with: **Next step →** for the top item: `/new-spec <drafted title>` (or create the issue first and `/spec-from-issue`). Re-audit next quarter and compare.
