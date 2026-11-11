---
name: resolve-merge-conflict
description: Resolve git merge/rebase conflicts safely — understand both intents, never silently drop one side, validate with tests after. Use whenever a merge, rebase or cherry-pick reports conflicts.
---

# Resolve a merge conflict

A conflict is two intents colliding. The job is preserving BOTH intents — not making the markers go away. The most dangerous outcome is not a broken build; it's a resolution that compiles and silently discards someone's work.

## 1. Understand before touching

```bash
git status                              # full list of conflicted files
git log --oneline --left-right HEAD...MERGE_HEAD -- <file>   # what each side did
git diff --base <file>                  # 3-way view: base vs ours vs theirs
```

For each conflicted file answer: what was OUR change trying to do, what was THEIRS trying to do? If you can't answer both, read the involved commits/MRs first (`git show <sha>`). Never resolve a hunk whose intent you don't understand.

## 2. Resolve by intent, not by side

- Both intents compatible → combine them (the common case: two additions in the same region).
- Genuinely exclusive (same line, different logic) → the resolution is a DECISION: check which MR/SDD is authoritative; when unclear, ask the user or the author of the other side — a guess here is silent sabotage.
- `git checkout --ours` / `--theirs` on whole files: only when one side is provably obsolete (e.g. file deleted by an accepted refactor) — state that reasoning in the merge commit message.

## 3. The traps beyond the markers

- **Semantic conflicts git can't see**: their side renamed a method your new code calls; both sides added the same migration version number; both bumped the same dependency differently. Compile + full test run is the only detector — a conflict-free merge can still be broken.
- Lockfiles / generated files: don't hand-merge — regenerate (`uv lock`, `npm install`, rebuild) after resolving the source files.
- Formatting-vs-logic conflicts: re-run the formatter after resolving; don't manually reconstruct style.

## 4. Validate

1. No leftover markers: `git grep -nE '^(<{7}|={7}|>{7})'`
2. Build + full test suite — not just the conflicted modules (semantic conflicts, above).
3. Review your own resolution diff: `git diff HEAD` before committing — does it preserve both intents?

## Rules

- Never resolve by deleting a test that conflicts — that's one of the intents.
- Long-running branch conflicting massively: prefer rebase in small steps (`git rebase -i`) or ask whether re-implementing on top of current main is cheaper than a 40-file resolution.
- After a hairy resolution, mention in the MR which files had non-trivial decisions — reviewers must look there.
